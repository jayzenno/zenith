package com.zenplayer.app.player

import android.content.Context
import android.util.Log
import android.view.View
import com.zenplayer.app.data.settings.PlayerEngineChoice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "ZenSession"

data class PlaybackProgress(val positionMs: Long, val durationMs: Long)

/**
 * Coordinates the concrete player engines and owns the playback state that the UI observes.
 *
 * Responsibilities:
 *  - start the preferred engine, fall back once to the other engine on a hard error or on a
 *    start timeout (no ping-pong, no half-initialised players),
 *  - expose a single stable [status] / [engine] / [progress] stream,
 *  - render the correct host view for the currently active engine.
 */
class ZenPlayerSession(
    private val hostContext: Context,
    preferredEngine: PlayerEngineChoice,
    userAgent: String?
) : ZenPlayerController {

    private data class Request(val uri: String, val title: String?, val subtitleUrl: String?)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val policy = FallbackPolicy(preferredEngine)
    private val resolvedUserAgent = userAgent?.takeIf { it.isNotBlank() } ?: PlayerDefaults.USER_AGENT

    private val _status = MutableStateFlow<PlaybackStatus>(PlaybackStatus.Idle)
    override val status: StateFlow<PlaybackStatus> = _status.asStateFlow()

    private val _engine = MutableStateFlow(preferredEngine)
    val engine: StateFlow<PlayerEngineChoice> = _engine.asStateFlow()

    private val _progress = MutableStateFlow(PlaybackProgress(0L, 0L))
    val progress: StateFlow<PlaybackProgress> = _progress.asStateFlow()

    private var exo: ExoPlayerController? = null
    private var vlc: VlcPlayerController? = null
    private var active: ZenPlayerController = controllerFor(policy.engineToStart())

    private var statusJob: Job? = null
    private var watchdog: Job? = null
    private var ticker: Job? = null
    private var request: Request? = null
    private var starts = 0

    init {
        bind(active)
        startTicker()
    }

    private fun controllerFor(choice: PlayerEngineChoice): ZenPlayerController =
        if (choice == PlayerEngineChoice.EXO) {
            exo ?: ExoPlayerController(hostContext, resolvedUserAgent).also { exo = it }
        } else {
            vlc ?: VlcPlayerController(hostContext, resolvedUserAgent).also { vlc = it }
        }

    private fun bind(controller: ZenPlayerController) {
        statusJob?.cancel()
        statusJob = scope.launch {
            controller.status.collect { incoming ->
                if (controller !== active) return@collect
                _status.value = incoming
                if (incoming is PlaybackStatus.Error) onEngineFailure(incoming)
            }
        }
    }

    override fun play(uri: String, title: String?, subtitleUrl: String?) {
        request = Request(uri, title, subtitleUrl)
        startStream()
    }

    /** Manual retry after an error/timeout. Reuses the current fallback decision. */
    fun retry() {
        if (request != null) startStream()
    }

    private fun startStream() {
        val req = request ?: return
        starts++
        val engine = policy.engineToStart()
        Log.d(
            TAG,
            "start #$starts engine=$engine host=${PlayerDiagnostics.hostOf(req.uri)} " +
                "type=${PlayerDiagnostics.detectStreamType(req.uri)} fallbackAvailable=${policy.canFallback()}"
        )
        watchdog?.cancel()
        val controller = controllerFor(engine)
        active = controller
        _engine.value = engine
        _progress.value = PlaybackProgress(0L, 0L)
        bind(controller)
        controller.play(req.uri, req.title, req.subtitleUrl)
        armWatchdog(controller)
    }

    private fun armWatchdog(controller: ZenPlayerController) {
        // Never arm for a controller that has already been superseded (e.g. a fallback that
        // happened synchronously while startStream was still returning).
        if (controller !== active) return
        watchdog = scope.launch {
            val settled = withTimeoutOrNull(PlayerDefaults.START_TIMEOUT_MS) {
                controller.status.first {
                    it is PlaybackStatus.Playing || it is PlaybackStatus.Ready ||
                        it is PlaybackStatus.Error || it is PlaybackStatus.Ended
                }
            }
            if (controller !== active) return@launch
            when (settled) {
                is PlaybackStatus.Playing, is PlaybackStatus.Ready,
                is PlaybackStatus.Error, is PlaybackStatus.Ended -> Unit

                else -> {
                    Log.w(
                        TAG,
                        "start timeout after ${PlayerDefaults.START_TIMEOUT_MS}ms engine=${_engine.value} " +
                            "status=${_status.value}"
                    )
                    if (policy.canFallback()) switchToFallback()
                    else _status.value = PlaybackStatus.Timeout
                }
            }
        }
    }

    private fun onEngineFailure(error: PlaybackStatus.Error) {
        Log.w(TAG, "engine failure: ${error.message} fallbackAvailable=${policy.canFallback()}")
        if (policy.canFallback()) switchToFallback()
    }

    private fun switchToFallback() {
        if (!policy.canFallback()) return
        watchdog?.cancel()
        val fallback = policy.markPrimaryFailed()
        Log.w(TAG, "automatic fallback to engine=$fallback")
        val controller = controllerFor(fallback)
        active = controller
        _engine.value = fallback
        _progress.value = PlaybackProgress(0L, 0L)
        bind(controller)
        val req = request ?: return
        _status.value = PlaybackStatus.Loading
        controller.play(req.uri, req.title, req.subtitleUrl)
        armWatchdog(controller)
    }

    private fun startTicker() {
        ticker = scope.launch {
            while (isActive) {
                _progress.value = PlaybackProgress(
                    active.positionMs().coerceAtLeast(0L),
                    active.durationMs().coerceAtLeast(0L)
                )
                delay(500L)
            }
        }
    }

    fun videoView(hostContext: Context): View = when (val controller = active) {
        is ExoPlayerController -> controller.playerView(hostContext)
        is VlcPlayerController -> controller.videoLayout(hostContext)
        else -> View(hostContext)
    }

    override fun toggle() = active.toggle()
    override fun pause() = active.pause()
    override fun resume() = active.resume()
    override fun seekBy(deltaMs: Long) = active.seekBy(deltaMs)
    override fun positionMs(): Long = active.positionMs()
    override fun durationMs(): Long = active.durationMs()

    override fun release() {
        watchdog?.cancel()
        ticker?.cancel()
        statusJob?.cancel()
        exo?.release()
        vlc?.release()
        exo = null
        vlc = null
        scope.cancel()
    }
}
