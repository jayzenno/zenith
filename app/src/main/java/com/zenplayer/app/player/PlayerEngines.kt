package com.zenplayer.app.player

import android.content.Context
import android.util.Log
import android.view.View
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "ZenPlayer"

sealed interface PlaybackStatus {
    data object Idle : PlaybackStatus
    data object Loading : PlaybackStatus
    data object Buffering : PlaybackStatus
    data object Ready : PlaybackStatus
    data object Playing : PlaybackStatus
    data object Paused : PlaybackStatus
    data object Ended : PlaybackStatus

    /** No usable playback within the configured start window; raised by the session, not the engine. */
    data object Timeout : PlaybackStatus
    data class Error(val message: String?) : PlaybackStatus
}

interface ZenPlayerController {
    val status: StateFlow<PlaybackStatus>
    fun play(uri: String, title: String?, subtitleUrl: String?)
    fun toggle()
    fun pause()
    fun resume()
    fun seekBy(deltaMs: Long)
    fun positionMs(): Long
    fun durationMs(): Long
    fun release()
}

class ExoPlayerController(context: Context, userAgent: String? = null) : ZenPlayerController {

    private val _status = MutableStateFlow<PlaybackStatus>(PlaybackStatus.Idle)
    override val status: StateFlow<PlaybackStatus> = _status.asStateFlow()

    /**
     * True once the player has rendered content at least once. Used to distinguish the
     * initial load ([PlaybackStatus.Loading]) from a mid-playback rebuffer
     * ([PlaybackStatus.Buffering]).
     */
    private var hasStarted = false

    private val configuredUserAgent: String? = userAgent?.takeIf { it.isNotBlank() }

    private val mediaSourceFactory = DefaultMediaSourceFactory(context).apply {
        val agent = configuredUserAgent
        setDataSourceFactory(
            DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(PlayerDefaults.CONNECT_TIMEOUT_MS)
                .setReadTimeoutMs(PlayerDefaults.READ_TIMEOUT_MS)
                .apply { agent?.let { setUserAgent(it) } }
        )
    }

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setRenderersFactory(DefaultRenderersFactory(context).setEnableDecoderFallback(true))
        .setMediaSourceFactory(mediaSourceFactory)
        .setHandleAudioBecomingNoisy(true)
        .build()

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            Log.d(
                TAG,
                "state=${stateName(playbackState)} playWhenReady=${player.playWhenReady} isPlaying=${player.isPlaying}"
            )
            syncStatus()
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            syncStatus()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) hasStarted = true
            syncStatus()
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "error ${describeError(error)}")
            _status.value = PlaybackStatus.Error(error.errorCodeName)
        }
    }

    init {
        player.addListener(listener)
    }

    /**
     * Single source of truth for the ExoPlayer status. All listener callbacks funnel through
     * here so ordering between `onPlaybackStateChanged` / `onPlayWhenReadyChanged` /
     * `onIsPlayingChanged` can never leave the state inconsistent.
     */
    private fun syncStatus() {
        val error = player.playerError
        if (error != null) {
            _status.value = PlaybackStatus.Error(error.errorCodeName)
            return
        }
        _status.value = when (player.playbackState) {
            Player.STATE_IDLE ->
                if (player.playWhenReady && player.mediaItemCount > 0) PlaybackStatus.Loading
                else PlaybackStatus.Idle

            Player.STATE_BUFFERING ->
                if (hasStarted) PlaybackStatus.Buffering else PlaybackStatus.Loading

            Player.STATE_READY -> {
                hasStarted = true
                if (player.playWhenReady) PlaybackStatus.Playing else PlaybackStatus.Paused
            }

            Player.STATE_ENDED -> PlaybackStatus.Ended
            else -> _status.value
        }
    }

    override fun play(uri: String, title: String?, subtitleUrl: String?) {
        Log.d(
            TAG,
            "play host=${PlayerDiagnostics.hostOf(uri)} type=${PlayerDiagnostics.detectStreamType(uri)} " +
                "ua=${configuredUserAgent ?: "default"}"
        )
        hasStarted = false
        _status.value = PlaybackStatus.Loading
        player.setMediaItem(MediaItem.Builder().setUri(uri).build())
        player.prepare()
        player.playWhenReady = true
    }

    override fun toggle() {
        if (player.isPlaying) pause() else resume()
    }

    override fun pause() {
        player.pause()
    }

    override fun resume() {
        player.play()
    }

    override fun seekBy(deltaMs: Long) {
        val duration = durationMs()
        val target = (player.currentPosition + deltaMs).coerceAtLeast(0L)
        player.seekTo(if (duration > 0) target.coerceAtMost(duration) else target)
    }

    override fun positionMs(): Long = player.currentPosition.coerceAtLeast(0L)

    override fun durationMs(): Long {
        val duration = player.duration
        return if (duration == C.TIME_UNSET || duration < 0) 0L else duration
    }

    fun playerView(context: Context): View = PlayerView(context).apply {
        player = this@ExoPlayerController.player
        // Zenith renders its own TV-first controls; the built-in controller would
        // duplicate them and steal D-pad focus.
        useController = false
        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
        isFocusable = false
        isFocusableInTouchMode = false
        keepScreenOn = true
        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
    }

    override fun release() {
        player.removeListener(listener)
        player.release()
    }

    private fun stateName(state: Int): String = when (state) {
        Player.STATE_IDLE -> "IDLE"
        Player.STATE_BUFFERING -> "BUFFERING"
        Player.STATE_READY -> "READY"
        Player.STATE_ENDED -> "ENDED"
        else -> "UNKNOWN($state)"
    }

    private fun describeError(error: PlaybackException): String {
        val builder = StringBuilder("code=${error.errorCodeName}")
        val cause = error.cause
        if (cause != null) {
            builder.append(" cause=").append(cause.javaClass.simpleName)
            if (cause is HttpDataSource.InvalidResponseCodeException) {
                builder.append(" http=").append(cause.responseCode)
            }
            val message = PlayerDiagnostics.sanitizeMessage(cause.message)
            if (message.isNotEmpty()) builder.append(" msg=").append(message)
        }
        return builder.toString()
    }
}
