package com.zenplayer.app.di

import android.content.Context
import com.zenplayer.app.data.db.ZenDatabase
import com.zenplayer.app.data.repo.ChannelRepository
import com.zenplayer.app.data.repo.EpgRepository
import com.zenplayer.app.data.repo.IptvRepository
import com.zenplayer.app.data.settings.SettingsRepository

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val db: ZenDatabase by lazy { ZenDatabase.get(appContext) }
    val settings: SettingsRepository by lazy { SettingsRepository(appContext) }
    val iptvRepo: IptvRepository by lazy {
        IptvRepository(appContext, db.providerDao(), db.channelDao(), db.epgDao(), db)
    }
    val channels: ChannelRepository by lazy { ChannelRepository(db.channelDao()) }
    val epg: EpgRepository by lazy { EpgRepository(db.epgDao()) }

    fun destroyAll() {
        db.close()
    }
}