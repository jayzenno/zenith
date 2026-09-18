package com.zenplayer.app.ui.music

import com.zenplayer.app.data.model.MediaType
import com.zenplayer.app.di.AppContainer
import com.zenplayer.app.ui.browse.MediaListViewModel

class MusicViewModel(container: AppContainer) : MediaListViewModel(container, MediaType.MUSIC)