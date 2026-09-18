package com.zenplayer.app.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zenplayer.app.ui.epg.EpgViewModel
import com.zenplayer.app.ui.livetv.LiveTvViewModel
import com.zenplayer.app.ui.music.MusicViewModel
import com.zenplayer.app.ui.settings.SettingsViewModel
import com.zenplayer.app.ui.vod.VodViewModel

class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(LiveTvViewModel::class.java) -> LiveTvViewModel(container) as T
        modelClass.isAssignableFrom(VodViewModel::class.java) -> VodViewModel(container) as T
        modelClass.isAssignableFrom(MusicViewModel::class.java) -> MusicViewModel(container) as T
        modelClass.isAssignableFrom(EpgViewModel::class.java) -> EpgViewModel(container) as T
        modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(container) as T
        else -> throw IllegalArgumentException("Unbekanntes ViewModel: ${modelClass.name}")
    }
}