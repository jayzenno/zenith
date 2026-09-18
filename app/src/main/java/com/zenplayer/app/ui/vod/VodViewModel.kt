package com.zenplayer.app.ui.vod

import com.zenplayer.app.data.model.MediaType
import com.zenplayer.app.di.AppContainer
import com.zenplayer.app.ui.browse.MediaListViewModel

class VodViewModel(container: AppContainer) : MediaListViewModel(container, MediaType.VOD)