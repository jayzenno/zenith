package com.zenplayer.app.ui.epg

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenplayer.app.data.model.Channel
import com.zenplayer.app.data.settings.ZenSettings
import com.zenplayer.app.data.settings.recentOnlyVis
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

    /**
     * Real database channel at grid position [vi]. The Guide hands this to the
     * player so OK/play starts the actual provider stream — never a fabricated
     * placeholder channel.
     */
    fun channelAt(vi: Int): Channel? = dbChannels.getOrNull(vi)

    init {
        val s = settings.value
        val vis = currentVis(s)
        if (vis.isNotEmpty()) {
            row = vis.indexOf(s.currentChannel).coerceAtLeast(0)
            // Anchors on the effective display day so the Guide opens around "now" even
            // before 05:00 (the current programme then lives in the previous window).
            ecol = epgProgAt(vis[row], day(), nowMin)
        }
        viewModelScope.launch {
            container.channels.allLiveChannels().collect { list ->
                // Full list, deliberately NOT capped (masterplan rule #4). The grid is
                // virtualized; every provider channel stays reachable in the Guide.
                dbChannels = list
                val mapped = withContext(Dispatchers.Default) {
                    list.mapIndexed { i, ch -> mapDbChannel(ch, i) }
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
        // Exactly ONE group is active — the settings writers keep the flags and the
        // provider category mutually exclusive. "Favoriten" with an empty favorites set
        // shows the honest empty state instead of falling back to "all channels".
        return when {
            s.epgFavsOnly -> if (s.epgFavs.isEmpty()) emptyList() else all.filter { it in s.epgFavs }
            s.epgCategoryGroup != null -> epgCategoryVis(s.epgCategoryGroup, dbChannels)
            s.epgRecentOnly -> recentOnlyVis(s.epgRecent, dbChannels)
            else -> all
        }
    }

    /**
     * Cycles the active Guide group: Alle Sender -> Favoriten -> Zuletzt gesehen -> every
     * real provider category -> Alle. Empty virtual groups are skipped automatically, so on
     * a fresh install the cycle goes straight into the provider categories. Exactly one
     * group stays active; see [com.zenplayer.app.data.settings.SettingsRepository.setEpgActiveGroup]
     * and [com.zenplayer.app.data.settings.SettingsRepository.setEpgCategoryGroup].
     */
    fun cycleGroup() {
        val s = settings.value
        val cats = epgCategories(dbChannels)
        val next = epgNextGroup(
            EpgGroupState(s.epgFavsOnly, s.epgRecentOnly, s.epgCategoryGroup),
            cats,
            includeFavs = s.epgFavs.isNotEmpty(),
            includeRecent = s.epgRecent.isNotEmpty()
        )
        val nextName = epgGroupCategoryName(next)
        val isCat = next.startsWith("cat:")
        viewModelScope.launch {
            when (next) {
                "all" -> container.settings.setEpgActiveGroup("all")
                "favs" -> container.settings.setEpgActiveGroup("favs")
                "recent" -> container.settings.setEpgActiveGroup("recent")
                else -> container.settings.setEpgCategoryGroup(nextName)
            }
        }
        // Snap synchronously to the NEXT group's channel list; the settings flow updates
        // asynchronously, so computing the target here from the current snapshot is exact.
        val nextVis = when {
            next == "favs" -> EPG_CHANNELS.indices.filter { it in s.epgFavs }
            next == "recent" -> recentOnlyVis(s.epgRecent, dbChannels)
            next == "all" -> EPG_CHANNELS.indices.toList()
            else -> epgCategoryVis(nextName, dbChannels)
        }
        snapTo(nextVis)
        toast(
            when {
                isCat -> "Kategorie: $nextName"
                next == "favs" -> "Favoriten"
                next == "recent" -> "Zuletzt gesehen"
                else -> "Alle Sender"
            } + " aktiv"
        )
    }

    /** Human-readable label of the currently active Guide group. */
    fun activeGroupLabel(): String = when {
        settings.value.epgFavsOnly -> "Favoriten"
        settings.value.epgRecentOnly -> "Zuletzt gesehen"
        settings.value.epgCategoryGroup != null -> settings.value.epgCategoryGroup.orEmpty()
        else -> "Alle Sender"
    }

    /**
     * Re-anchors the selection for [vis]: the current channel is preferred if present,
     * otherwise the first row; the programme column snaps to "now".
     */
    private fun snapTo(vis: List<Int>) {
        if (vis.isEmpty()) {
            row = 0
            ecol = 0
            return
        }
        val r = vis.indexOf(currentChannel())
        row = if (r < 0) 0 else r
        ecol = epgProgAt(vis[row], day(), nowMin)
    }

    /**
     * The effective display day: the preference ([ZenSettings.epgDay]) is clamped to the
     * stored range and then shifted back by one while local time is before 05:00
     * ([effectiveDay]). Without the shift the Guide could never open "around now" in the
     * early morning — the current moment would sit in the previous 05:00–05:00 window,
     * which the old hard clamp at 0 made unreachable.
     */
    fun day(): Int {
        val pref = settings.value.epgDay.coerceIn(0, EPG_DAY_SLOTS - 1)
        return effectiveDay(pref, nowMin)
    }

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
        // Day stepping works on the PREFERENCE day (0..EPG_DAY_SLOTS-1); the displayed
        // window is the effective day derived from it (morning hours shift back by one).
        val pref = settings.value.epgDay.coerceIn(0, EPG_DAY_SLOTS - 1)
        val np = clampInt(pref + d, 0, EPG_DAY_SLOTS - 1)
        val target = effectiveDay(np, nowMin)
        viewModelScope.launch { container.settings.setEpgDay(np) }
        val vis = currentVis()
        if (vis.isNotEmpty()) {
            ecol = epgProgAt(vis[row.coerceIn(0, vis.size - 1)], target, nowMin)
        }
        refreshPrograms(target)
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
        val cur = settings.value.epgFavs.toMutableSet()
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
        viewModelScope.launch {
            container.settings.setEpgActiveGroup(if (next) "favs" else "all")
        }
        // Synchronous snap from the current snapshot (the settings flow is async).
        val nextVis = if (next) {
            EPG_CHANNELS.indices.filter { it in settings.value.epgFavs }
        } else {
            EPG_CHANNELS.indices.toList()
        }
        snapTo(nextVis)
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

    fun ctxItems(vi: Int): List<CtxItem> = epgCtxItems(isFav(vi))

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
            "info" -> {
                val p = epgProgramsFor(vi, day()).getOrNull(ecol)
                toast(programChannelLabel(vi) + " · " + (p?.t ?: "–"))
            }
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
