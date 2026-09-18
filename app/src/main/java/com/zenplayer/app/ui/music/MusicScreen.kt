package com.zenplayer.app.ui.music

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zenplayer.app.data.model.MediaType
import com.zenplayer.app.di.AppContainer
import com.zenplayer.app.di.AppViewModelFactory
import com.zenplayer.app.ui.browse.BrowseScaffold
import com.zenplayer.app.ui.browse.ProviderChips
import com.zenplayer.app.ui.components.EmptyState
import com.zenplayer.app.ui.components.GridRow
import com.zenplayer.app.ui.components.PosterChannelCard

@Composable
fun MusicScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onPlay: (Long, MediaType, String) -> Unit
) {
    val vm: MusicViewModel = viewModel(factory = remember { AppViewModelFactory(container) })
    val providers by vm.providers.collectAsStateWithLifecycle(initialValue = emptyList())
    val activeId by vm.activeProviderId.collectAsStateWithLifecycle(initialValue = -1L)
    val rows by vm.rows.collectAsStateWithLifecycle(initialValue = emptyList())
    val activeName = providers.firstOrNull { it.id == activeId }?.name

    BrowseScaffold(
        title = "Musik",
        subtitle = activeName?.let { "Anbieter: $it" },
        onBack = onBack,
        headerExtra = {
            if (providers.size > 1) ProviderChips(providers, activeId, vm::selectProvider)
        }
    ) {
        when {
            providers.isEmpty() -> {
                item {
                    EmptyState(
                        title = "Kein Anbieter verbunden",
                        message = "Füge deinen ersten IPTV-Anbieter hinzu, um Musikkanäle zu hören.",
                        actionLabel = "Einstellungen öffnen",
                        onAction = onOpenSettings,
                        modifier = Modifier.fillMaxWidth().height(520.dp)
                    )
                }
            }

            rows.isEmpty() -> {
                item {
                    EmptyState(
                        title = "Keine Kanäle",
                        message = "Der Anbieter wurde noch nicht synchronisiert. Starte die Synchronisierung in den Einstellungen.",
                        actionLabel = "Einstellungen öffnen",
                        onAction = onOpenSettings,
                        modifier = Modifier.fillMaxWidth().height(520.dp)
                    )
                }
            }

            else -> {
                items(rows.size, key = { "music-$activeId-$it" }) { index ->
                    val row = rows[index]
                    GridRow(
                        title = row.category,
                        items = row.channels,
                        key = { it.id }
                    ) { channel ->
                        PosterChannelCard(channel) {
                            onPlay(activeId, MediaType.MUSIC, channel.id)
                        }
                    }
                }
            }
        }
    }
}