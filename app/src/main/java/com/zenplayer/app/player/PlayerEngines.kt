package com.zenplayer.app.player

import android.content.Context
import android.util.Log
import android.view.View
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "ZenPlayer"

sealed interface PlaybackStatus {
    data object Idle : PlaybackStatus
    data object Loading : PlaybackStatus
    data object Ready : PlaybackStatus
    data object Playing : PlaybackStatus
    data object Paused : PlaybackStatus
    data class Error(val message: String?) : PlaybackStatus
}

interface ZenPlayerController {
    val status: StateFlow<PlaybackStatus>
    fun play(uri: String, title: String?, subtitleUrl: String?)
    fun toggle()
    fun pause()
    fun resume()
    fun seekBy(deltaMs: Long)
    fun release()
}

class ExoPlayerController(context: Context) : ZenPlayerController {

    private val _status = MutableStateFlow<PlaybackStatus>(PlaybackStatus.Idle)
    override val status: StateFlow<PlaybackStatus> = _status.asStateFlow()

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setHandleAudioBecomingNoisy(true)
        .build()

    init {
        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onIsLoadingChanged(isLoading: Boolean) {
                Log.d(TAG, "ExoPlayer onIsLoadingChanged: $isLoading, playWhenReady=${player.playWhenReady}")
                if (isLoading && player.playWhenReady) _status.value = PlaybackStatus.Loading
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val state = when (playbackState) {
                    androidx.media3.common.Player.STATE_IDLE -> "IDLE"
                    androidx.media3.common.Player.STATE_BUFFERING -> "BUFFERING"
                    androidx.media3.common.Player.STATE_READY -> "READY"
                    androidx.media3.common.Player.STATE_ENDED -> "ENDED"
                    else -> "UNKNOWN($playbackState)"
                }
                Log.d(TAG, "ExoPlayer onPlaybackStateChanged: $state, isPlaying=${player.isPlaying}")
                when (playbackState) {
                    androidx.media3.common.Player.STATE_READY -> {
                        _status.value = if (player.isPlaying) PlaybackStatus.Playing else PlaybackStatus.Ready
                    }

                    androidx.media3.common.Player.STATE_BUFFERING -> _status.value = PlaybackStatus.Loading
                    androidx.media3.common.Player.STATE_ENDED -> _status.value = PlaybackStatus.Idle
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                Log.d(TAG, "ExoPlayer onPlayWhenReadyChanged: $playWhenReady, playbackState=${player.playbackState}")
                if (player.playbackState == androidx.media3.common.Player.STATE_READY) {
                    _status.value = if (playWhenReady) PlaybackStatus.Playing else PlaybackStatus.Paused
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e(TAG, "ExoPlayer error: ${error.errorCodeName} - ${error.message}")
                _status.value = PlaybackStatus.Error(error.errorCodeName)
            }
        })
    }

    override fun play(uri: String, title: String?, subtitleUrl: String?) {
        Log.d(TAG, "ExoPlayer play() called: uri=$uri, title=$title")
        val builder = MediaItem.Builder().setUri(uri).build()
        player.setMediaItem(builder)
        player.prepare()
        player.playWhenReady = true
        _status.value = PlaybackStatus.Loading
        Log.d(TAG, "ExoPlayer prepared and playWhenReady=true")
    }

    override fun toggle() {
        if (player.isPlaying) pause() else resume()
    }

    override fun pause() {
        player.pause()
    }

    override fun resume() {
        player.play()
        if (player.playbackState == androidx.media3.common.Player.STATE_READY) {
            _status.value = PlaybackStatus.Playing
        } else {
            _status.value = PlaybackStatus.Loading
        }
    }

    override fun seekBy(deltaMs: Long) {
        player.seekTo(player.currentPosition + deltaMs)
    }

    fun playerView(context: Context): View = PlayerView(context).apply {
        this.player = this@ExoPlayerController.player
        useController = true
        controllerAutoShow = true
    }

    override fun release() {
        player.release()
    }
}