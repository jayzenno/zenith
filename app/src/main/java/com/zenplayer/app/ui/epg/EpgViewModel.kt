package com.zenplayer.app.ui.epg

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenplayer.app.data.model.Channel
import com.zenplayer.app.data.settings.ZenSettings
import com.zenplayer.app.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime

enum class EpgLayer { CHS, GRID, CONTEXT }

enum class EpgMoveDir { LEFT, RIGHT, UP, DOWN }

data class CtxItem(val action: String, val title: String, val key: String)

class EpgViewModel(private val container: AppContainer) : ViewModel() {

    val settings: StateFlow<ZenSettings> = container.settings.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ZenSettings())

    var nowMin by mutableStateOf(com.zenplayer.app.ui.epg.nowMin())
        private set

    var row by mutableStateOf(0)
        private set

    var ecol by mutableStateOf(0)
        private set

    var layer by mutableStateOf(EpgLayer.CHS)
        private set

    var previewOn by mutableStateOf(false)
        private set

    var ctxOpen by mutableStateOf(false)
        private set

    var ctxIndex by mutableStateOf(0)
        private set

    var toastText by mutableStateOf<String?>(null)
        private set

    var onOpenPlayer: ((Int) -> Unit)? = null

    private var dbChannels: List<Channel> = emptyList()
    private var _ctxLayer = EpgLayer.CHS
    private var lastEnter = 0L
    private var entFired = false
    private var entJob: Job? = null

    init {
        val s = settings.value
        val vis = currentVis(s)
        if (vis.isNotEmpty()) {
            row = vis.indexOf(s.currentChannel).coerceAtLeast(0)
            ecol = epgProgAt(vis[row], s.epgDay, nowMin)
        }
        viewModelScope.launch {
            container.channels.allLiveChannels().collect { list ->
                val limited = list.take(MAX_EPG_CHANNELS)
                dbChannels = limited
                val mapped = withContext(Dispatchers.Default) {
                    limited.mapIndexed { i, ch -> mapDbChannel(ch, i) }
                }
                EpgStore.updateChannels(mapped)
                if (row == 0 && mapped.isNotEmpty()) {
                    val r = currentVis().indexOf(currentChannel())
                    row = if (r < 0) 0 else r
                    ecol = epgProgAt(row, day(), nowMin)
                }
                refreshPrograms(day())
            }
        }
        viewModelScope.launch {
            try {
                container.iptvRepo.providers().forEach { provider ->
                    container.iptvRepo.syncEpg(provider)
                }
                refreshPrograms(day())
            } catch (_: Exception) {
            }
        }
    }

    private fun refreshPrograms(day: Int) {
        viewModelScope.launch {
            val all = try {
                container.epg.programsForWindow(day)
            } catch (_: Exception) {
                emptyList()
            }
            if (all.isEmpty()) return@launch
            val byEpgId = all.groupBy { it.channelId }
            val size = minOf(dbChannels.size, EPG_CHANNELS.size)
            for (vi in 0 until size) {
                val ch = dbChannels[vi]
                val epgId = ch.extra?.takeIf { it.isNotBlank() } ?: ch.id
                val progs = byEpgId[epgId] ?: continue
                EpgStore.setPrograms(vi, day, mapDbPrograms(progs, day))
            }
        }
    }

    fun tick() {
        nowMin = com.zenplayer.app.ui.epg.nowMin()
    }

    fun currentVis(s: ZenSettings = settings.value): List<Int> {
        val all = EPG_CHANNELS.indices.toList()
        val filtered = if (!s.epgFavsOnly || s.epgFavs.isEmpty()) all else all.filter { it in s.epgFavs }
        return filtered.take(MAX_EPG_CHANNELS)
    }

    fun day(): Int = settings.value.epgDay.coerceIn(0, EPG_DAY_SLOTS - 1)

    fun currentChannel(): Int {
        if (EPG_CHANNELS.isEmpty()) return 0
        return settings.value.currentChannel.coerceIn(0, EPG_CHANNELS.size - 1)
    }

    fun isFav(vi: Int): Boolean = vi in settings.value.epgFavs

    fun progsForRow(vi: Int = currentVis().getOrNull(row) ?: 0): List<EpgProgram> =
        epgProgramsFor(vi, day())

    private fun clampEcol(vis: List<Int>) {
        val max = (epgProgramsFor(vis.getOrNull(row) ?: 0, day()).size - 1).coerceAtLeast(0)
        ecol = clampInt(ecol, 0, max)
    }

    fun move(dir: EpgMoveDir) {
        val vis = currentVis()
        if (vis.isEmpty()) return
        when (dir) {
            EpgMoveDir.DOWN -> row = clampInt(row + 1, 0, vis.size - 1)
            EpgMoveDir.UP -> row = clampInt(row - 1, 0, vis.size - 1)
            EpgMoveDir.RIGHT -> ecol = clampInt(ecol + 1, 0, epgProgramsFor(vis[row], day()).size - 1)
            EpgMoveDir.LEFT -> ecol = clampInt(ecol - 1, 0, epgProgramsFor(vis[row], day()).size - 1)
        }
        if (dir == EpgMoveDir.UP || dir == EpgMoveDir.DOWN) {
            if (settings.value.epgAutoNow) {
                ecol = epgProgAt(vis[row], day(), nowMin)
            } else {
                clampEcol(vis)
            }
        }
        setCh(vis[row])
    }

    fun enterGrid() {
        val vis = currentVis()
        if (vis.isEmpty()) return
        row = clampInt(row, 0, vis.size - 1)
        ecol = epgProgAt(vis[row], day(), nowMin)
        layer = EpgLayer.GRID
    }

    fun enterPress() {
        val now = System.currentTimeMillis()
        if (now - lastEnter < 480) {
            lastEnter = 0
            entJob?.cancel()
            entJob = null
            doEnter()
            return
        }
        lastEnter = now
        entFired = false
        entJob = viewModelScope.launch {
            delay(520)
            entFired = true
            openCtx()
        }
    }

    fun enterRelease() {
        entJob?.cancel()
        entJob = null
        if (!entFired) doEnter()
    }

    private fun doEnter() {
        val vi = currentVis().getOrNull(row) ?: return
        if (previewOn) {
            previewOn = false
            playChannel(vi)
        } else if (!settings.value.epgPipp) {
            playChannel(vi)
        } else {
            previewOn = true
            setCh(vi)
        }
    }

    fun playChannel(vi: Int) {
        previewOn = false
        onOpenPlayer?.invoke(vi)
    }

    fun setDayStep(d: Int) {
        val np = clampInt(day() + d, 0, EPG_DAY_SLOTS - 1)
        viewModelScope.launch { container.settings.setEpgDay(np) }
        val vis = currentVis()
        if (vis.isNotEmpty()) {
            ecol = epgProgAt(vis[row.coerceIn(0, vis.size - 1)], np, nowMin)
        }
        refreshPrograms(np)
        toast(dayToast(np))
    }

    fun snapNow() {
        val vis = currentVis()
        if (vis.isEmpty()) return
        val r = vis.indexOf(currentChannel())
        row = if (r < 0) 0 else r
        ecol = epgProgAt(vis[row], day(), nowMin)
        toast("Springe zu jetzt")
    }

    fun toggleFav(vi: Int) {
        val cur = settings.value.epgFavs.ifEmpty { setOf(0, 2, 4, 6) }.toMutableSet()
        val removed = cur.remove(vi)
        if (removed) {
            viewModelScope.launch { container.settings.setEpgFavs(cur) }
            toast("Aus Favoriten entfernt")
        } else {
            cur.add(vi)
            viewModelScope.launch { container.settings.setEpgFavs(cur) }
            toast("Zu Favoriten hinzugefügt")
        }
    }

    fun toggleFavsOnly() {
        val next = !settings.value.epgFavsOnly
        viewModelScope.launch { container.settings.setEpgFavsOnly(next) }
    }

    private fun setCh(vi: Int) {
        if (vi == settings.value.currentChannel) return
        viewModelScope.launch { container.settings.setCurrentChannel(vi) }
    }

    fun openCtx() {
        val vis = currentVis()
        val vi = vis.getOrNull(row) ?: vis.firstOrNull() ?: currentChannel()
        _ctxLayer = layer
        ctxIndex = 0
        ctxOpen = true
        layer = EpgLayer.CONTEXT
    }

    fun ctxItems(vi: Int): List<CtxItem> {
        val p = epgProgramsFor(vi, day()).getOrNull(ecol)
        val replay = p != null && p.s <= nowMin && p.e > nowMin
        val items = mutableListOf(
            CtxItem("play", "Wiedergabe", "OK"),
            CtxItem("fav", if (isFav(vi)) "Aus Favoriten entfernen" else "Zu Favoriten hinzufügen", "♥")
        )
        if (replay) items.add(CtxItem("replay", "Von Anfang an (Replay)", "ENTF"))
        items.add(CtxItem("remind", "Merken", "M"))
        items.add(CtxItem("info", "Senderinfo", "I"))
        items.add(CtxItem("rec", "Aufnahme planen", "R"))
        if (vi > 0) items.add(CtxItem("foco", "Nur dieser Sender", "ESC"))
        return items
    }

    fun ctxMove(dir: EpgMoveDir) {
        val items = ctxItems(currentVis().getOrNull(row) ?: 0)
        if (items.isEmpty()) return
        val delta = if (dir == EpgMoveDir.DOWN || dir == EpgMoveDir.RIGHT) 1 else -1
        ctxIndex = (ctxIndex + delta + items.size) % items.size
    }

    fun ctxAct(action: String) {
        val vis = currentVis()
        val vi = vis.getOrNull(row) ?: vis.firstOrNull() ?: currentChannel()
        hideCtx()
        when (action) {
            "play" -> playChannel(vi)
            "fav" -> toggleFav(vi)
            "replay" -> toast("Replay ab Sendungsbeginn gestartet")
            "remind" -> toast("Zur Merkliste hinzugefügt")
            "info" -> {
                val p = epgProgramsFor(vi, day()).getOrNull(ecol)
                toast(EPG_CHANNELS[vi].name + " · Kanal " + EPG_CHANNELS[vi].num + " · " + (p?.t ?: "–"))
            }
            "rec" -> {
                val p = epgProgramsFor(vi, day()).getOrNull(ecol)
                toast("Aufnahme um " + (if (p != null) hh(p.s) else "–") + " Uhr geplant")
            }
            "foco" -> toast("Fokus auf " + EPG_CHANNELS[vi].name)
        }
    }

    fun hideCtx() {
        ctxOpen = false
        layer = _ctxLayer
    }

    fun backKey(onExit: () -> Unit) {
        if (ctxOpen) {
            hideCtx()
            return
        }
        when (layer) {
            EpgLayer.GRID -> {
                if (previewOn) {
                    previewOn = false
                } else {
                    layer = EpgLayer.CHS
                    val vis = currentVis()
                    if (vis.isNotEmpty()) ecol = epgProgAt(vis[row], day(), nowMin)
                }
            }
            EpgLayer.CHS -> onExit()
            EpgLayer.CONTEXT -> hideCtx()
        }
    }

    private fun toast(text: String) {
        toastText = text
    }

    fun clearToast() {
        toastText = null
    }
}
