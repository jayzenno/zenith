package com.zenplayer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenplayer.app.data.settings.ZenSettings
import com.zenplayer.app.ui.navigation.ZenNavHost
import com.zenplayer.app.ui.navigation.ZenShell
import com.zenplayer.app.ui.theme.ZenTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as ZenPlayerApplication
        val container = app.container
        setContent {
            val settings by container.settings.settingsFlow.collectAsStateWithLifecycle(initialValue = ZenSettings())
            ZenTheme(settings) {
                ZenShell(settings, container) { navController ->
                    ZenNavHost(container, navController)
                }
            }
        }
    }
}