package com.zenplayer.app.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenplayer.app.data.model.Provider
import com.zenplayer.app.data.model.ProviderType
import com.zenplayer.app.data.net.HttpEngine
import com.zenplayer.app.data.repo.SyncResult
import com.zenplayer.app.data.settings.PlayerEngineChoice
import com.zenplayer.app.data.settings.ZenSettings
import com.zenplayer.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProviderDraft(
    val name: String = "",
    val url: String = "",
    val username: String = "",
    val password: String = "",
    val mac: String = "",
    val epgUrl: String = "",
    val type: ProviderType = ProviderType.XTREAM
)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val settings: StateFlow<ZenSettings> = container.settings.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ZenSettings())

    val providers: StateFlow<List<Provider>> = container.iptvRepo.providersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            settings.collect { s ->
                HttpEngine.userAgent = s.userAgent ?: "ZenPlayer/0.1 (Android TV)"
                HttpEngine.applyTimeouts(
                    s.syncTimeoutSeconds.coerceAtLeast(5),
                    (s.syncTimeoutSeconds * 3).coerceAtLeast(15)
                )
            }
        }
    }

    private val _syncingProviderId = MutableStateFlow<Long?>(null)
    val syncingProviderId: StateFlow<Long?> = _syncingProviderId

    private val _lastSyncResult = MutableStateFlow<SyncResult?>(null)
    val lastSyncResult: StateFlow<SyncResult?> = _lastSyncResult

    var draft by mutableStateOf<ProviderDraft?>(null)
        private set

    var editingProviderId by mutableStateOf<Long?>(null)
        private set

    var pendingDelete by mutableStateOf<Provider?>(null)
        private set

    fun openAddProvider() {
        draft = ProviderDraft()
        editingProviderId = null
    }

    fun openEditProvider(provider: Provider) {
        draft = ProviderDraft(
            name = provider.name,
            url = provider.url,
            username = provider.username.orEmpty(),
            password = provider.password.orEmpty(),
            mac = provider.mac.orEmpty(),
            epgUrl = provider.epgUrl.orEmpty(),
            type = provider.type
        )
        editingProviderId = provider.id
    }

    fun closeDialog() {
        draft = null
        editingProviderId = null
    }

    fun updateDraft(transform: (ProviderDraft) -> ProviderDraft) {
        draft?.let { draft = transform(it) }
    }

    fun saveDraft() {
        val d = draft ?: return
        viewModelScope.launch {
            val id = editingProviderId
            val provider = if (id == null || id == 0L) {
                val newId = container.iptvRepo.addProvider(
                    Provider(
                        name = d.name.trim(),
                        type = d.type,
                        url = d.url.trim(),
                        username = d.username.trim().ifBlank { null },
                        password = d.password.trim().ifBlank { null },
                        mac = d.mac.trim().ifBlank { null },
                        epgUrl = d.epgUrl.trim().ifBlank { null }
                    )
                )
                container.settings.setSelectedProvider(newId)
                container.iptvRepo.getProvider(newId)
            } else {
                val updated = Provider(
                    id = id,
                    name = d.name.trim(),
                    type = d.type,
                    url = d.url.trim(),
                    username = d.username.trim().ifBlank { null },
                    password = d.password.trim().ifBlank { null },
                    mac = d.mac.trim().ifBlank { null },
                    epgUrl = d.epgUrl.trim().ifBlank { null }
                )
                container.iptvRepo.updateProvider(updated)
                updated
            }
            draft = null
            editingProviderId = null
            provider?.let { syncProvider(it, auto = true) }
        }
    }

    fun syncProvider(provider: Provider, auto: Boolean = false) {
        viewModelScope.launch {
            try {
                _syncingProviderId.value = provider.id
                _lastSyncResult.value = container.iptvRepo.syncProvider(provider)
            } catch (e: Exception) {
                _lastSyncResult.value = SyncResult(false, e.message ?: "Sync fehlgeschlagen")
            } finally {
                _syncingProviderId.value = null
            }
        }
    }

    fun syncEpg(provider: Provider) {
        viewModelScope.launch {
            try {
                _lastSyncResult.value = container.iptvRepo.syncEpg(provider)
            } catch (e: Exception) {
                _lastSyncResult.value = SyncResult(false, e.message ?: "EPG-Sync fehlgeschlagen")
            }
        }
    }

    fun clearLastSyncResult() {
        _lastSyncResult.value = null
    }

    fun requestDelete(provider: Provider) {
        pendingDelete = provider
    }

    fun cancelDelete() {
        pendingDelete = null
    }

    fun confirmDelete() {
        val provider = pendingDelete ?: return
        viewModelScope.launch {
            container.iptvRepo.deleteProvider(provider)
            container.settings.setSelectedProvider(-1L)
            pendingDelete = null
        }
    }

    fun setThemePreset(id: String) {
        viewModelScope.launch { container.settings.setThemePreset(id) }
    }

    fun setAccent(argb: Long?) {
        viewModelScope.launch { container.settings.setCustomAccent(argb) }
    }

    fun setGlass(frost: Float, blurDp: Float, sheen: Float) {
        viewModelScope.launch { container.settings.setGlass(frost, blurDp, sheen) }
    }

    fun setBackgroundType(type: String) {
        viewModelScope.launch { container.settings.setBackgroundType(type) }
    }

    fun setBackgroundAnimated(animated: Boolean) {
        viewModelScope.launch { container.settings.setBackgroundAnimated(animated) }
    }

    fun setBackgroundBlur(blurDp: Float) {
        viewModelScope.launch { container.settings.setBackgroundBlur(blurDp) }
    }

    fun setFocusEffect(effect: String) {
        viewModelScope.launch { container.settings.setFocusEffect(effect) }
    }

    fun setCardStyle(style: String) {
        viewModelScope.launch { container.settings.setCardStyle(style) }
    }

    fun setNavStyle(style: String) {
        viewModelScope.launch { container.settings.setNavStyle(style) }
    }

    fun setHomeStyle(style: String) {
        viewModelScope.launch { container.settings.setHomeStyle(style) }
    }

    fun setFontSize(size: String) {
        viewModelScope.launch { container.settings.setFontSize(size) }
    }

    fun setDensityScale(scale: Float) {
        viewModelScope.launch { container.settings.setDensityScale(scale) }
    }

    fun setHomeScale(scale: Float) {
        viewModelScope.launch { container.settings.setHomeScale(scale) }
    }

    fun setBrowseScale(scale: Float) {
        viewModelScope.launch { container.settings.setBrowseScale(scale) }
    }

    fun setEpgScale(scale: Float) {
        viewModelScope.launch { container.settings.setEpgScale(scale) }
    }

    fun setPlayerScale(scale: Float) {
        viewModelScope.launch { container.settings.setPlayerScale(scale) }
    }

    fun setSettingsScale(scale: Float) {
        viewModelScope.launch { container.settings.setSettingsScale(scale) }
    }

    fun setEpgSize(size: Float) {
        viewModelScope.launch { container.settings.setEpgSize(size) }
    }

    fun setEpgColW(colW: Float) {
        viewModelScope.launch { container.settings.setEpgColW(colW) }
    }

    fun setEpgFavsOnly(value: Boolean) {
        // Exactly one Guide group active at a time (All / Favoriten / Zuletzt gesehen). The
        // combined setter prevents a torn state where favorites AND recent are both on.
        viewModelScope.launch { container.settings.setEpgActiveGroup(if (value) "favs" else "all") }
    }

    fun setEpgAutoNow(value: Boolean) {
        viewModelScope.launch { container.settings.setEpgAutoNow(value) }
    }

    fun setEpgPipp(value: Boolean) {
        viewModelScope.launch { container.settings.setEpgPipp(value) }
    }

    fun setEpgLogos(value: Boolean) {
        viewModelScope.launch { container.settings.setEpgLogos(value) }
    }

    fun setEngine(engine: PlayerEngineChoice) {
        viewModelScope.launch { container.settings.setPlayerEngine(engine) }
    }

    fun setPreferHd(value: Boolean) {
        viewModelScope.launch { container.settings.setPreferHd(value) }
    }

    fun setSyncTimeout(seconds: Int) {
        viewModelScope.launch { container.settings.setSyncTimeout(seconds) }
    }

    fun setUserAgent(agent: String?) {
        viewModelScope.launch { container.settings.setUserAgent(agent) }
    }
}