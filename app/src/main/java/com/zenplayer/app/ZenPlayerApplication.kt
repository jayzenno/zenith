package com.zenplayer.app

import android.app.Application
import com.zenplayer.app.di.AppContainer

class ZenPlayerApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onTerminate() {
        super.onTerminate()
        container.destroyAll()
    }
}