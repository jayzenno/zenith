package com.zenplayer.app.player

import android.content.Context
import android.net.Uri
import android.view.View
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

class VlcPlayerController(context: Context) : ZenPlayerController {

    private val _status = MutableStateFlow<PlaybackStatus>(PlaybackStatus.Idle)
    override val status: StateFlow<PlaybackStatus> = _status.asStateFlow()

    private val libVLC: LibVLC = LibVLC(
        context,
        listOf("--no-stats", "--no-drop-late-frames", "--network-caching=800")
    )

    private var mediaPlayer: MediaPlayer? = null

    override fun play(uri: String, title: String?, subtitleUrl: String?) {
        releaseMediaPlayer()
        val player = MediaPlayer(libVLC)
        mediaPlayer = player
        _status.value = PlaybackStatus.Loading

        player.setEventListener(object : MediaPlayer.Event.Listener {
            override fun onEvent(event: MediaPlayer.Event) {
                when (event.type) {
                    MediaPlayer.Event.Playing -> _status.value = PlaybackStatus.Playing
                    MediaPlayer.Event.Paused -> _status.value = PlaybackStatus.Paused
                    MediaPlayer.Event.Stopped -> _status.value = PlaybackStatus.Idle
                    MediaPlayer.Event.EncounteredError -> _status.value = PlaybackStatus.Error("VLC Error")
                    MediaPlayer.Event.Buffering -> _status.value = PlaybackStatus.Loading
                    else -> {}
                }
            }
        })

        val media = Media(libVLC, Uri.parse(uri))
        media.setHWDecoderEnabled(true, false)
        subtitleUrl?.let { media.addOption(":sub-file=${it.replace(" ", "%20")}") }
        player.media = media
        attachVideoLayout()
        player.play()
    }

    override fun toggle() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
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

    private var videoLayout: VLCVideoLayout? = null

    fun videoLayout(context: Context): View {
        if (videoLayout == null) {
            videoLayout = VLCVideoLayout(context)
        }
        videoLayout?.let { layout ->
            mediaPlayer?.attachViews(layout, null, false, false)
        }
        return videoLayout!!
    }

    private fun attachVideoLayout() {
        val layout = videoLayout
        val player = mediaPlayer
        if (layout != null && player != null) {
            player.attachViews(layout, null, false, false)
        }
    }

    private fun releaseMediaPlayer() {
        val player = mediaPlayer
        mediaPlayer = null
        player?.stop()
        player?.detachViews()
        player?.release()
    }

    override fun release() {
        releaseMediaPlayer()
        libVLC.release()
    }
}