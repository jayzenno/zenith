package com.zenplayer.app.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenplayer.app.data.model.Channel
import com.zenplayer.app.data.model.EpgProgram
import com.zenplayer.app.data.model.MediaType
import com.zenplayer.app.di.AppContainer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PlayerUi {
    data object Empty : PlayerUi
    data class Active(
        val channel: Channel,
        val index: Int,
        val total: Int
    ) : PlayerUi
}

class PlayerViewModel(
    container: AppContainer,
    providerId: Long,
    mediaType: MediaType,
    startChannelId: String
) : ViewModel() {

    private val channelsRepo = container.channels
    private val epgRepo = container.epg

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    private val _index = MutableStateFlow(0)

    val state: StateFlow<PlayerUi> = combine(_channels, _index) { list, index ->
        if (list.isEmpty()) {
            PlayerUi.Empty
        } else {
            val safeIndex = index.coerceIn(0, list.size - 1)
            PlayerUi.Active(list[safeIndex], safeIndex, list.size)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerUi.Empty)

    init {
        viewModelScope.launch {
            channelsRepo.channelsOfType(providerId, mediaType).collect { list ->
                _channels.value = list
                val found = list.indexOfFirst { it.id == startChannelId }
                _index.value = if (found >= 0) found else 0
            }
        }
    }

    fun zap(delta: Int) {
        val size = _channels.value.size
        if (size == 0) return
        _index.value = ((_index.value + delta) % size + size) % size
    }

    fun upcoming(channelId: String): Flow<List<EpgProgram>> = epgRepo.upcoming(channelId)
}