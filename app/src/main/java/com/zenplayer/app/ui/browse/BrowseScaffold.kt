package com.zenplayer.app.ui.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zenplayer.app.data.model.Provider
import com.zenplayer.app.ui.components.ZenChip
import com.zenplayer.app.ui.components.ZenScreenHeading

@Composable
fun BrowseScaffold(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    headerExtra: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(34.dp)
    ) {
        item { ZenScreenHeading(title, subtitle, onBack) }
        if (headerExtra != null) {
            item { headerExtra() }
        }
        content()
    }
}

@Composable
fun ProviderChips(
    providers: List<Provider>,
    selectedId: Long,
    onSelect: (Long) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        providers.forEach { provider ->
            ZenChip(
                label = provider.name,
                selected = provider.id == selectedId,
                onClick = { onSelect(provider.id) }
            )
        }
    }
}