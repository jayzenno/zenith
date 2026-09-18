package com.zenplayer.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zenplayer.app.data.model.Provider
import com.zenplayer.app.data.model.ProviderType
import com.zenplayer.app.data.settings.PlayerEngineChoice
import com.zenplayer.app.data.settings.ZenSettings
import com.zenplayer.app.di.AppContainer
import com.zenplayer.app.di.AppViewModelFactory
import com.zenplayer.app.ui.components.zenFocusEffect
import com.zenplayer.app.ui.theme.LocalFocusEffect
import com.zenplayer.app.ui.theme.LocalZenColors
import com.zenplayer.app.ui.theme.ThemePresets
import com.zenplayer.app.ui.theme.ZenPreset
import com.zenplayer.app.ui.theme.toFocusEffect
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TABS = listOf(Triple("darstellung", "Erscheinungsbild", 0), Triple("wiedergabe", "Wiedergabe", 1), Triple("profil", "Profil", 2))

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(container: AppContainer, onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = remember { AppViewModelFactory(container) })
    val settings by vm.settings.collectAsStateWithLifecycle()
    val providers by vm.providers.collectAsStateWithLifecycle()
    val syncingId by vm.syncingProviderId.collectAsStateWithLifecycle()
    val lastResult by vm.lastSyncResult.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf("darstellung") }
    val scroll = rememberScrollState()

    val colors = LocalZenColors.current
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 28.dp, vertical = 26.dp)
    ) {
        Text("Einstellungen", fontSize = 32.sp, fontWeight = FontWeight.Black, color = colors.onSurface)
        Spacer(Modifier.height(14.dp))
        TabBar(tab, colors) { tab = it }
        Spacer(Modifier.height(22.dp))
        lastResult?.let { result ->
            SyncBanner(result, colors, vm::clearLastSyncResult)
            Spacer(Modifier.height(14.dp))
        }
        when (tab) {
            "darstellung" -> AppearanceTab(settings, vm, colors)
            "wiedergabe" -> PlaybackTab(settings, vm, colors)
            else -> ProviderTab(providers, syncingId, vm, colors)
        }
        Spacer(Modifier.height(40.dp))
    }
    vm.draft?.let { ProviderDialog(it, vm) }
    vm.pendingDelete?.let { ConfirmDeleteDialog(it, vm) }
}

@Composable
private fun TabBar(active: String, colors: com.zenplayer.app.ui.theme.ZenColors, onSelect: (String) -> Unit) {
    val line = colors.onSurface.copy(alpha = 0.12f)
    val panel = colors.surface.copy(alpha = 0.42f)
    Row(
        modifier = Modifier.clip(RoundedCornerShape(50)).background(panel).border(1.dp, line, RoundedCornerShape(50)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TABS.forEach { (id, label, _) ->
            val selected = id == active
            Box(
                modifier = Modifier.clip(RoundedCornerShape(50)).background(if (selected) Color.White.copy(alpha = 0.12f) else Color.Transparent).clickable { onSelect(id) }.padding(horizontal = 18.dp, vertical = 8.dp)
            ) { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (selected) colors.onSurface else colors.onSurface.copy(alpha = 0.55f)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppearanceTab(settings: ZenSettings, vm: SettingsViewModel, colors: com.zenplayer.app.ui.theme.ZenColors) {
    Panel("Live-Vorschau", colors) { PreviewMini(settings, colors) }
    Spacer(Modifier.height(14.dp))
    Panel("Farbthema", colors) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ThemePresets.all.forEach { preset -> ThemeSwatch(preset, preset.id == settings.themePresetId, colors) { vm.setThemePreset(preset.id) } }
        }
    }
    Spacer(Modifier.height(14.dp))
    Panel("Akzentfarbe", colors) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            accentChoices.forEach { argb -> AccentSwatch(argb, settings.customAccentArgb == argb) { vm.setAccent(argb) } }
            ChoiceBtn("Standard", settings.customAccentArgb == null, colors) { vm.setAccent(null) }
        }
    }
    Spacer(Modifier.height(14.dp))
    Panel("Liquid Glass", colors) {
        StepRow("Frost", "${(settings.glassFrost * 100).toInt()} %", colors,
            { vm.setGlass((settings.glassFrost - 0.05f).coerceAtLeast(0.1f), settings.glassBlurDp, settings.glassSheen) },
            { vm.setGlass((settings.glassFrost + 0.05f).coerceAtMost(0.8f), settings.glassBlurDp, settings.glassSheen) })
        StepRow("Weichzeichner", "${settings.glassBlurDp.toInt()} dp", colors,
            { vm.setGlass(settings.glassFrost, (settings.glassBlurDp - 2f).coerceAtLeast(2f), settings.glassSheen) },
            { vm.setGlass(settings.glassFrost, (settings.glassBlurDp + 2f).coerceAtMost(64f), settings.glassSheen) })
        StepRow("Glanzlichter", "${(settings.glassSheen * 100).toInt()} %", colors,
            { vm.setGlass(settings.glassFrost, settings.glassBlurDp, (settings.glassSheen - 0.05f).coerceAtLeast(0.2f)) },
            { vm.setGlass(settings.glassFrost, settings.glassBlurDp, (settings.glassSheen + 0.05f).coerceAtMost(1f)) })
    }
    Spacer(Modifier.height(14.dp))
    Panel("Hintergrund", colors) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            backgroundTypes.forEach { (id, label) ->
                ChoiceBtn(label, settings.backgroundType == id, colors) { vm.setBackgroundType(id) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ChoiceBtn("Animiert", settings.backgroundAnimated, colors) { vm.setBackgroundAnimated(true) }
            ChoiceBtn("Statisch", !settings.backgroundAnimated, colors) { vm.setBackgroundAnimated(false) }
        }
        Spacer(Modifier.height(12.dp))
        StepRow("Hintergrund-Weichzeichner", "${settings.backgroundBlurDp.toInt()} dp", colors,
            { vm.setBackgroundBlur((settings.backgroundBlurDp - 2f).coerceAtLeast(0f)) },
            { vm.setBackgroundBlur((settings.backgroundBlurDp + 2f).coerceAtMost(64f)) })
    }
    Spacer(Modifier.height(14.dp))
    Panel("Fokus-Effekt", colors) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            focusEffects.forEach { (id, label) ->
                ChoiceBtn(label, settings.focusEffect == id, colors) { vm.setFocusEffect(id) }
            }
        }
    }
    Spacer(Modifier.height(14.dp))
    Panel("Karten-Stil", colors) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            cardStyles.forEach { (id, label) ->
                ChoiceBtn(label, settings.cardStyle == id, colors) { vm.setCardStyle(id) }
            }
        }
    }
    Spacer(Modifier.height(14.dp))
    Panel("Navigation", colors) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            navStyles.forEach { (id, label) ->
                ChoiceBtn(label, settings.navStyle == id, colors) { vm.setNavStyle(id) }
            }
        }
    }
    Spacer(Modifier.height(14.dp))
    Panel("Startbildschirm", colors) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            homeStyles.forEach { (id, label) ->
                ChoiceBtn(label, settings.homeStyle == id, colors) { vm.setHomeStyle(id) }
            }
        }
    }
    Spacer(Modifier.height(14.dp))
    Panel("Schriftgroesse", colors) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            fontSizes.forEach { (id, label) ->
                ChoiceBtn(label, settings.fontSize == id, colors) { vm.setFontSize(id) }
            }
        }
    }
    Spacer(Modifier.height(14.dp))
    Panel("Skalierung", colors) {
        StepRow("Globale Dichte", "${(settings.densityScale * 100).toInt()} %", colors,
            { vm.setDensityScale((settings.densityScale - 0.05f).coerceAtLeast(0.5f)) },
            { vm.setDensityScale((settings.densityScale + 0.05f).coerceAtMost(2f)) })
        StepRow("Start", "${(settings.homeScale * 100).toInt()} %", colors,
            { vm.setHomeScale((settings.homeScale - 0.05f).coerceAtLeast(0.5f)) },
            { vm.setHomeScale((settings.homeScale + 0.05f).coerceAtMost(2f)) })
        StepRow("Mediathek", "${(settings.browseScale * 100).toInt()} %", colors,
            { vm.setBrowseScale((settings.browseScale - 0.05f).coerceAtLeast(0.5f)) },
            { vm.setBrowseScale((settings.browseScale + 0.05f).coerceAtMost(2f)) })
        StepRow("EPG", "${(settings.epgScale * 100).toInt()} %", colors,
            { vm.setEpgScale((settings.epgScale - 0.05f).coerceAtLeast(0.5f)) },
            { vm.setEpgScale((settings.epgScale + 0.05f).coerceAtMost(2f)) })
        StepRow("Player", "${(settings.playerScale * 100).toInt()} %", colors,
            { vm.setPlayerScale((settings.playerScale - 0.05f).coerceAtLeast(0.5f)) },
            { vm.setPlayerScale((settings.playerScale + 0.05f).coerceAtMost(2f)) })
        StepRow("Einstellungen", "${(settings.settingsScale * 100).toInt()} %", colors,
            { vm.setSettingsScale((settings.settingsScale - 0.05f).coerceAtLeast(0.5f)) },
            { vm.setSettingsScale((settings.settingsScale + 0.05f).coerceAtMost(2f)) })
    }
    Spacer(Modifier.height(14.dp))
    Panel("EPG-Optionen", colors) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ChoiceBtn("Nur Favoriten", settings.epgFavsOnly, colors) { vm.setEpgFavsOnly(!settings.epgFavsOnly) }
            ChoiceBtn("Auto-Jetzt", settings.epgAutoNow, colors) { vm.setEpgAutoNow(!settings.epgAutoNow) }
            ChoiceBtn("Vorschau", settings.epgPipp, colors) { vm.setEpgPipp(!settings.epgPipp) }
            ChoiceBtn("Logos", settings.epgLogos, colors) { vm.setEpgLogos(!settings.epgLogos) }
        }
    }
}

@Composable
private fun PlaybackTab(settings: ZenSettings, vm: SettingsViewModel, colors: com.zenplayer.app.ui.theme.ZenColors) {
    Panel("Abspiel-Engine", colors) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PlayerEngineChoice.entries.forEach { engine -> ChoiceBtn(engine.label, settings.playerEngine == engine, colors) { vm.setEngine(engine) } }
        }
    }
    Spacer(Modifier.height(14.dp))
    Panel("Bevorzugte Qualitaet", colors) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ChoiceBtn("HD", settings.preferHd, colors) { vm.setPreferHd(true) }
            ChoiceBtn("Beste verfuegbar", !settings.preferHd, colors) { vm.setPreferHd(false) }
        }
    }
}

@Composable
private fun ProviderTab(providers: List<Provider>, syncingId: Long?, vm: SettingsViewModel, colors: com.zenplayer.app.ui.theme.ZenColors) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        providers.forEach { provider ->
            ProviderCard(provider, syncingId == provider.id, colors, { vm.syncProvider(provider) }, { vm.syncEpg(provider) }, { vm.openEditProvider(provider) }, { vm.requestDelete(provider) })
        }
        AddProviderBtn(vm::openAddProvider, colors)
    }
}

@Composable
private fun Panel(label: String, colors: com.zenplayer.app.ui.theme.ZenColors, content: @Composable ColumnScope.() -> Unit) {
    val line = colors.onSurface.copy(alpha = 0.12f)
    val panel = colors.surface.copy(alpha = 0.42f)
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(panel).border(1.dp, line, RoundedCornerShape(16.dp)).padding(20.dp)
    ) {
        Text(label.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Black, color = colors.onSurface.copy(alpha = 0.6f), letterSpacing = 1.sp)
        Spacer(Modifier.height(16.dp))
        content()
    }
}

@Composable
private fun PreviewMini(settings: ZenSettings, colors: com.zenplayer.app.ui.theme.ZenColors) {
    val preset = ThemePresets.all.firstOrNull { it.id == settings.themePresetId } ?: ThemePresets.all.first()
    val line = colors.onSurface.copy(alpha = 0.12f)
    Box(
        modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(14.dp)).background(colors.background).border(1.dp, line, RoundedCornerShape(14.dp))
    ) {
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(preset.blobA.copy(alpha = 0.35f), Color.Transparent), center = Offset(0.2f, 0.8f), radius = 220f)))
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(preset.blobB.copy(alpha = 0.28f), Color.Transparent), center = Offset(0.85f, 0.25f), radius = 180f)))
        Column(Modifier.align(Alignment.TopStart).padding(18.dp)) {
            Text("Dein Fernsehen", fontSize = 18.sp, fontWeight = FontWeight.Black, color = colors.onSurface)
            Text("${preset.name} · ${settings.playerEngine.label} · Glas ${(settings.glassFrost * 100).toInt()}% · Fokus ${settings.focusEffect.replaceFirstChar { it.uppercase() }}", fontSize = 11.sp, color = colors.onSurface.copy(alpha = 0.55f))
            Spacer(Modifier.height(14.dp))
            Box(Modifier.clip(RoundedCornerShape(50)).background(colors.accentBrush).padding(horizontal = 16.dp, vertical = 7.dp)) {
                Text("Ansehen", fontSize = 11.sp, fontWeight = FontWeight.Black, color = colors.background)
            }
        }
    }
}

@Composable
private fun ThemeSwatch(preset: ZenPreset, selected: Boolean, colors: com.zenplayer.app.ui.theme.ZenColors, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier.size(58.dp).clip(RoundedCornerShape(50)).background(Brush.linearGradient(listOf(preset.blobA, preset.accentStart, preset.accentEnd))).border(2.dp, if (selected) colors.onSurface else Color.Transparent, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) { if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp)) }
        Spacer(Modifier.height(6.dp))
        Text(preset.name, fontSize = 10.sp, color = colors.onSurface.copy(alpha = 0.55f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AccentSwatch(argb: Long, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalZenColors.current
    Box(
        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(50)).background(Color(argb.toInt())).border(2.dp, if (selected) colors.onSurface else Color.Transparent, RoundedCornerShape(50)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF101010), modifier = Modifier.size(20.dp)) }
}

@Composable
private fun ChoiceBtn(label: String, selected: Boolean, colors: com.zenplayer.app.ui.theme.ZenColors, onClick: () -> Unit) {
    val focusEffect = LocalFocusEffect.current
    var focused by remember { mutableStateOf(false) }
    val line = colors.onSurface.copy(alpha = 0.12f)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Color.White.copy(alpha = 0.12f) else Color.Transparent)
            .border(1.dp, if (selected) Color.White.copy(alpha = 0.25f) else line, RoundedCornerShape(50))
            .zenFocusEffect(focused, focusEffect, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .focusable()
            .onFocusChanged { focused = it.isFocused && it.hasFocus }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selected) colors.onSurface else colors.onSurface.copy(alpha = 0.55f)) }
}

@Composable
private fun StepRow(label: String, value: String, colors: com.zenplayer.app.ui.theme.ZenColors, onDec: () -> Unit, onInc: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = colors.onSurface)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SmallBtn("-", colors, onDec)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.onSurface, modifier = Modifier.width(48.dp), maxLines = 1)
            SmallBtn("+", colors, onInc)
        }
    }
}

@Composable
private fun SmallBtn(label: String, colors: com.zenplayer.app.ui.theme.ZenColors, onClick: () -> Unit) {
    val focusEffect = LocalFocusEffect.current
    var focused by remember { mutableStateOf(false) }
    val line = colors.onSurface.copy(alpha = 0.12f)
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, line, RoundedCornerShape(50))
            .zenFocusEffect(focused, focusEffect, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .focusable()
            .onFocusChanged { focused = it.isFocused && it.hasFocus },
        contentAlignment = Alignment.Center
    ) { Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.onSurface) }
}

@Composable
private fun ProviderCard(provider: Provider, isSyncing: Boolean, colors: com.zenplayer.app.ui.theme.ZenColors, onSync: () -> Unit, onSyncEpg: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val line = colors.onSurface.copy(alpha = 0.12f)
    val panel = colors.surface.copy(alpha = 0.42f)
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(panel).border(1.dp, line, RoundedCornerShape(16.dp)).padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(colors.accentEnd.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Text(provider.type.name.take(3), fontSize = 11.sp, fontWeight = FontWeight.Black, color = colors.accentEnd)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(provider.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(provider.url, fontSize = 11.sp, color = colors.onSurface.copy(alpha = 0.55f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Letzte Synchronisierung: ${formattedDate(provider.lastUpdated)}", fontSize = 10.sp, color = colors.onSurface.copy(alpha = 0.45f))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (isSyncing) Text("Synchronisiert...", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.accentStart)
            else {
                ChoiceBtn("Sync", false, colors, onSync)
                ChoiceBtn("EPG", false, colors, onSyncEpg)
                ChoiceBtn("Bearbeiten", false, colors, onEdit)
                ChoiceBtn("Loeschen", false, colors, onDelete)
            }
        }
    }
}

@Composable
private fun AddProviderBtn(onClick: () -> Unit, colors: com.zenplayer.app.ui.theme.ZenColors) {
    val line = colors.onSurface.copy(alpha = 0.12f)
    val panel = colors.surface.copy(alpha = 0.42f)
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(panel).border(1.dp, line, RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(18.dp),
        contentAlignment = Alignment.Center
    ) { Text("+ Neuen Anbieter hinzufuegen", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.accentStart) }
}

@Composable
private fun ProviderDialog(draft: ProviderDraft, vm: SettingsViewModel) {
    val colors = LocalZenColors.current
    val line = colors.onSurface.copy(alpha = 0.12f)
    val panel = colors.surface.copy(alpha = 0.42f)
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f)), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.width(720.dp).clip(RoundedCornerShape(24.dp)).background(panel).border(1.dp, line, RoundedCornerShape(24.dp))) {
            Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(if (vm.editingProviderId != null) "Anbieter bearbeiten" else "Neuer Anbieter", fontSize = 24.sp, fontWeight = FontWeight.Black, color = colors.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProviderType.entries.forEach { type -> ChoiceBtn(type.displayName(), draft.type == type, colors) { vm.updateDraft { it.copy(type = type) } } }
                }
                ZenTextField("Name", draft.name, colors) { n -> vm.updateDraft { it.copy(name = n) } }
                ZenTextField("URL", draft.url, colors) { n -> vm.updateDraft { it.copy(url = n) } }
                if (draft.type == ProviderType.XTREAM) {
                    ZenTextField("Benutzername", draft.username, colors) { n -> vm.updateDraft { it.copy(username = n) } }
                    ZenTextField("Passwort", draft.password, colors) { n -> vm.updateDraft { it.copy(password = n) } }
                }
                if (draft.type == ProviderType.STALKER) ZenTextField("MAC-Adresse", draft.mac, colors) { n -> vm.updateDraft { it.copy(mac = n) } }
                ZenTextField("EPG-URL (optional)", draft.epgUrl, colors) { n -> vm.updateDraft { it.copy(epgUrl = n) } }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    ChoiceBtn("Abbrechen", false, colors, vm::closeDialog)
                    Spacer(Modifier.width(10.dp))
                    ChoiceBtn("Speichern", true, colors, vm::saveDraft)
                }
            }
        }
    }
}

@Composable
private fun ZenTextField(label: String, value: String, colors: com.zenplayer.app.ui.theme.ZenColors, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.accentStart,
            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
            focusedLabelColor = colors.accentStart,
            unfocusedLabelColor = colors.onSurface.copy(alpha = 0.55f),
            cursorColor = colors.accentStart,
            focusedTextColor = colors.onSurface,
            unfocusedTextColor = colors.onSurface,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ConfirmDeleteDialog(provider: Provider, vm: SettingsViewModel) {
    val colors = LocalZenColors.current
    val line = colors.onSurface.copy(alpha = 0.12f)
    val panel = colors.surface.copy(alpha = 0.42f)
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f)), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.width(620.dp).clip(RoundedCornerShape(24.dp)).background(panel).border(1.dp, line, RoundedCornerShape(24.dp))) {
            Column(Modifier.padding(34.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Anbieter loeschen?", fontSize = 24.sp, fontWeight = FontWeight.Black, color = colors.onSurface)
                Spacer(Modifier.height(12.dp))
                Text("${provider.name} und alle zugehoerigen Kanaele werden entfernt.", fontSize = 14.sp, color = colors.onSurface.copy(alpha = 0.55f))
                Spacer(Modifier.height(26.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ChoiceBtn("Abbrechen", false, colors, vm::cancelDelete)
                    ChoiceBtn("Loeschen", true, colors, vm::confirmDelete)
                }
            }
        }
    }
}

@Composable
private fun SyncBanner(result: com.zenplayer.app.data.repo.SyncResult, colors: com.zenplayer.app.ui.theme.ZenColors, onClear: () -> Unit) {
    val error = Color(0xFFFF2D78)
    val bg = if (result.ok) colors.accentStart.copy(alpha = 0.18f) else error.copy(alpha = 0.18f)
    val border = if (result.ok) colors.accentStart else error
    val text = if (result.ok) colors.accentStart else error
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, border.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable { onClear() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${if (result.ok) "OK" else "Fehler"}: ${result.message}" + if (result.channelCount > 0) " (${result.channelCount})" else "",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = text,
            modifier = Modifier.weight(1f)
        )
        Text("X", fontSize = 12.sp, fontWeight = FontWeight.Black, color = text)
    }
}

private val accentChoices = listOf(0xFF00E5C6L, 0xFF7B2FF7L, 0xFFFF2D78L, 0xFFFF9F1CL, 0xFFA3E635L, 0xFF38BDF8L)

private val backgroundTypes = listOf(
    "liquid" to "Liquid",
    "static" to "Statisch",
    "blur" to "Blur",
    "none" to "Aus",
    "wave" to "Welle",
    "aurora" to "Aurora",
    "grad" to "Farbverlauf",
    "holo" to "Holo",
    "plain" to "Einfarbig"
)

private val focusEffects = listOf(
    "ring" to "Ring",
    "glow" to "Glow",
    "zoom" to "Zoom",
    "bar" to "Bar",
    "corners" to "Ecken",
    "halo" to "Halo",
    "sweep" to "Sweep"
)

private val cardStyles = listOf("glass" to "Glas", "grad" to "Gradient", "flat" to "Flat")
private val navStyles = listOf("rail" to "Rail", "top" to "Oben", "tiles" to "Kacheln")
private val homeStyles = listOf("hub" to "Hub", "kanal" to "Kanal")
private val fontSizes = listOf("small" to "Klein", "normal" to "Normal", "large" to "Gross")

private fun ProviderType.displayName(): String = when (this) {
    ProviderType.M3U -> "M3U / M3U8"
    ProviderType.XTREAM -> "Xtream Codes"
    ProviderType.STALKER -> "Stalker Portal"
}

private fun formattedDate(ts: Long): String = if (ts <= 0) "-" else SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault()).format(Date(ts))
