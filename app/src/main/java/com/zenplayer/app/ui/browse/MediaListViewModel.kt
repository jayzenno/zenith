package com.zenplayer.app.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenplayer.app.data.model.Channel
import com.zenplayer.app.data.model.MediaType
import com.zenplayer.app.data.model.Provider
import com.zenplayer.app.di.AppContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

data class ChannelRow(
    val category: String,
    val channels: List<Channel>
)

@OptIn(ExperimentalCoroutinesApi::class)
abstract class MediaListViewModel(
    container: AppContainer,
    private val mediaType: MediaType
) : ViewModel() {

    protected val channelsRepo = container.channels
    protected val iptvRepo = container.iptvRepo
    protected val settingsRepo = container.settings

    val providers: Flow<List<Provider>> = iptvRepo.providersFlow

    private val _selectedProvider = MutableStateFlow<Long?>(null)

    val activeProviderId: Flow<Long> =
        combine(settingsRepo.settingsFlow, providers, _selectedProvider) { s, ps, sel ->
            sel ?: ps.firstOrNull { it.id == s.selectedProviderId }?.id ?: ps.firstOrNull()?.id ?: -1L
        }.distinctUntilChanged()

    val rows: Flow<List<ChannelRow>> = activeProviderId.flatMapLatest { providerId ->
        if (providerId < 0) {
            flowOf(emptyList())
        } else {
            channelsRepo.channelsOfType(providerId, mediaType).map { channelList ->
                channelList
                    .groupBy { it.category ?: "Alle Kanäle" }
                    .map { (category, list) -> ChannelRow(category, list) }
            }
        }
    }

    fun selectProvider(id: Long) {
        _selectedProvider.value = id
    }

    fun persistSelectedProvider(id: Long) {
        viewModelScope.launch { settingsRepo.setSelectedProvider(id) }
    }
}