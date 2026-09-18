package com.zenplayer.app.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.zenplayer.app.data.model.MediaType
import com.zenplayer.app.di.AppContainer
import com.zenplayer.app.ui.epg.EpgScreen
import com.zenplayer.app.ui.home.HomeScreen
import com.zenplayer.app.ui.livetv.LiveTvScreen
import com.zenplayer.app.ui.vod.VodScreen
import com.zenplayer.app.ui.music.MusicScreen
import com.zenplayer.app.ui.player.PlayerScreen
import com.zenplayer.app.ui.settings.SettingsScreen

@Composable
fun ZenNavHost(container: AppContainer, navController: NavHostController) {
    val back: () -> Unit = { navController.popBackStack() }

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable("live") {
            LiveTvScreen(
                container = container,
                onBack = back,
                onOpenSettings = { navController.navigate("settings") },
                onPlay = { providerId, mediaType, channelId ->
                    navController.navigate("player/$providerId/${mediaType.name}/${Uri.encode(channelId)}")
                }
            )
        }

        composable("vod") {
            VodScreen(
                container = container,
                onBack = back,
                onOpenSettings = { navController.navigate("settings") },
                onPlay = { providerId, mediaType, channelId ->
                    navController.navigate("player/$providerId/${mediaType.name}/${Uri.encode(channelId)}")
                }
            )
        }

        composable("music") {
            MusicScreen(
                container = container,
                onBack = back,
                onOpenSettings = { navController.navigate("settings") },
                onPlay = { providerId, mediaType, channelId ->
                    navController.navigate("player/$providerId/${mediaType.name}/${Uri.encode(channelId)}")
                }
            )
        }

        composable("epg") {
            EpgScreen(
                container = container,
                onExit = back,
                onOpenPlayer = { ch ->
                    navController.navigate("player/${ch.providerId}/${ch.mediaType.name}/${Uri.encode(ch.id)}")
                }
            )
        }

        composable("settings") {
            SettingsScreen(container = container, onBack = back)
        }

        composable(
            route = "player/{providerId}/{mediaType}/{channelId}",
            arguments = listOf(
                navArgument("providerId") { type = NavType.LongType },
                navArgument("mediaType") { type = NavType.StringType },
                navArgument("channelId") { type = NavType.StringType }
            )
        ) { entry ->
            val providerId = entry.arguments?.getLong("providerId") ?: -1L
            val mediaType = entry.arguments?.getString("mediaType")?.let { name ->
                runCatching { MediaType.valueOf(name) }.getOrNull()
            } ?: MediaType.LIVE
            val channelId = Uri.decode(entry.arguments?.getString("channelId").orEmpty())
            PlayerScreen(
                container = container,
                providerId = providerId,
                mediaType = mediaType,
                channelId = channelId,
                onBack = back
            )
        }
    }
}