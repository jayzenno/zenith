package com.zenplayer.app.player

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.View
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

private const val TAG = "ZenVlc"

class VlcPlayerController(context: Context, private val userAgent: String? = null) : ZenPlayerController {

    private val _status = MutableStateFlow<PlaybackStatus>(PlaybackStatus.Idle)
    override val status: StateFlow<PlaybackStatus> = _status.asStateFlow()

    private val libVLC: LibVLC = LibVLC(
        context,
        mutableListOf<String>().apply {
            add("--no-stats")
            add("--no-drop-late-frames")
            add("--network-caching=800")
            userAgent?.takeIf { it.isNotBlank() }?.let { add("--http-user-agent=$it") }
        }
    )

    private var mediaPlayer: MediaPlayer? = null
    private var media: Media? = null
    private var videoView: VLCVideoLayout? = null

    // attachViews() must be called exactly once per MediaPlayer/layout pair.
    private var viewsAttached = false
    private var hasStarted = false
    private var pendingPlay = false

    // Guards against late events from a previously released MediaPlayer mutating the state.
    private var generation = 0

    override fun play(uri: String, title: String?, subtitleUrl: String?) {
        Log.d(
            TAG,
            "play host=${PlayerDiagnostics.hostOf(uri)} type=${PlayerDiagnostics.detectStreamType(uri)}"
        )
        // Invalidate the previous player's listener before tearing it down so late events
        // can never mutate the new session's state.
        val gen = ++generation
        releaseMediaPlayer()

        val player = MediaPlayer(libVLC)
        mediaPlayer = player
        viewsAttached = false
        hasStarted = false
        pendingPlay = false
        _status.value = PlaybackStatus.Loading

        player.setEventListener(MediaPlayer.EventListener { event ->
            if (gen != generation) return@EventListener
            when (event.type) {
                MediaPlayer.Event.Playing -> {
                    hasStarted = true
                    _status.value = PlaybackStatus.Playing
                }

                MediaPlayer.Event.Paused -> _status.value = PlaybackStatus.Paused
                MediaPlayer.Event.Stopped -> _status.value = PlaybackStatus.Idle
                MediaPlayer.Event.EndReached -> _status.value = PlaybackStatus.Ended
                MediaPlayer.Event.EncounteredError ->
                    _status.value = PlaybackStatus.Error("VLC playback error")

                MediaPlayer.Event.Buffering -> _status.value = when {
                    event.buffering >= 100f && hasStarted -> PlaybackStatus.Playing
                    hasStarted -> PlaybackStatus.Buffering
                    else -> PlaybackStatus.Loading
                }

                else -> {}
            }
        })

        val m = Media(libVLC, Uri.parse(uri)).apply {
            setHWDecoderEnabled(true, false)
            addOption(":network-caching=800")
            userAgent?.takeIf { it.isNotBlank() }?.let { addOption(":http-user-agent=$it") }
            subtitleUrl?.let { addOption(":sub-file=${it.replace(" ", "%20")}") }
        }
        media = m
        player.media = m

        // Only start playback once the video surface is attached, otherwise LibVLC may
        // negotiate a video track with no output. If the layout does not exist yet the
        // play call is deferred until AndroidView materialises it.
        if (ensureViewsAttached()) {
            player.play()
        } else {
            pendingPlay = true
        }
    }

    override fun toggle() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    override fun pause() {
        mediaPlayer?.pause()
    }

    override fun resume() {
        mediaPlayer?.play()
    }

    override fun seekBy(deltaMs: Long) {
        val player = mediaPlayer ?: return
        player.time = (player.time + deltaMs).coerceAtLeast(0L)
    }

    override fun positionMs(): Long = mediaPlayer?.time?.coerceAtLeast(0L) ?: 0L

    override fun durationMs(): Long = mediaPlayer?.length?.takeIf { it > 0L } ?: 0L

    fun videoLayout(context: Context): View {
        val layout = videoView ?: VLCVideoLayout(context).also {
            it.isFocusable = false
            it.isFocusableInTouchMode = false
            videoView = it
        }
        ensureViewsAttached()
        if (pendingPlay) {
            pendingPlay = false
            mediaPlayer?.play()
        }
        return layout
    }

    private fun ensureViewsAttached(): Boolean {
        val layout = videoView ?: return false
        val player = mediaPlayer ?: return false
        if (viewsAttached) return true
        return runCatching {
            player.attachViews(layout, null, false, false)
            viewsAttached = true
            true
        }.getOrElse { error ->
            Log.e(TAG, "attachViews failed", error)
            false
        }
    }

    private fun releaseMediaPlayer() {
        val player = mediaPlayer
        mediaPlayer = null
        viewsAttached = false
        pendingPlay = false
        hasStarted = false

        runCatching { player?.stop() }
        runCatching { player?.detachViews() }
        runCatching { player?.release() }
        runCatching { media?.release() }
        media = null
    }

    override fun release() {
        generation++
        releaseMediaPlayer()
        libVLC.release()
    }
}
