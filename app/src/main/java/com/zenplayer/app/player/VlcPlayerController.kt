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

        val media = Media(libVLC, Uri.parse(uri))
        media.setHWDecoderEnabled(true, false)
        subtitleUrl?.let { media.addOption(":sub-file=${it.replace(" ", "%20")}") }
        player.media = media
        player.play()
        _status.value = PlaybackStatus.Playing
    }

    override fun toggle() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    override fun pause() {
        mediaPlayer?.pause()
        _status.value = PlaybackStatus.Paused
    }

    override fun resume() {
        mediaPlayer?.play()
        _status.value = PlaybackStatus.Playing
    }

    override fun seekBy(deltaMs: Long) {
        val player = mediaPlayer ?: return
        player.time = (player.time + deltaMs).coerceAtLeast(0L)
    }

    fun videoLayout(context: Context): View = VLCVideoLayout(context).also { layout ->
        mediaPlayer?.attachViews(layout, null, false, false)
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.stop()
        mediaPlayer?.detachViews()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun release() {
        releaseMediaPlayer()
        libVLC.release()
    }
}