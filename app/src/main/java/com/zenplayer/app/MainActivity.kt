package com.zenplayer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
            val navController = rememberNavController()
            val route = navController.currentBackStackEntryAsState().value?.destination?.route ?: "home"
            val immersivePlayer = route.startsWith("player")
            ZenTheme(settings, showBackdrop = !immersivePlayer) {
                ZenShell(settings, container, navController) { controller ->
                    ZenNavHost(container, controller)
                }
            }
        }
    }
}