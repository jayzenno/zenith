package com.zenplayer.app.ui.epg

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import com.zenplayer.app.data.model.Channel
import com.zenplayer.app.data.model.EpgProgram as DbEpgProgram
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.Calendar
import java.util.Date
import java.util.Locale

const val EPG_DAY_SLOTS = 7

const val EPG_START_MIN = 300
const val EPG_END_MIN = 300 + 1440
const val EPG_HOURS = 24

/**
 * There is deliberately NO hardcoded channel cap anymore. The Guide grid is virtualized
 * (`LazyColumn`), so a real IPTV playlist with thousands of channels is safe: only the
 * visible rows are composed. All provider channels are reachable — a hidden `MAX_EPG_CHANNELS`
 * would silently drop the rest (masterplan rule #4, Core-TV gate "no silent truncation").
 *
 * The horizontal axis is a shared `ScrollState` (header + every composed programme row),
 * so the full 24 h time line stays synchronized regardless of how many rows exist.
 */

data class EpgChannel(
    val name: String,
    val code: String,
    val colorStart: Long,
    val colorEnd: Long,
    val num: Int
)

data class EpgProgram(
    val s: Int,
    val e: Int,
    val t: String,
    val c: String
)

/**
 * Deliberately starts EMPTY. Only real provider channels are ever shown:
 * [EpgStore.updateChannels] replaces this list with the database content
 * (`EpgViewModel` collects `allLiveChannels()` and maps them here).
 *
 * Hardcoded demo channels / mock schedules were removed — in a real IPTV
 * app fake programme data must never appear in the Guide (masterplan rule #2,
 * Core-TV gate: "no production placeholders").
 */
val EPG_CHANNELS = mutableStateListOf<EpgChannel>()

object EpgStore {
    private val programs = mutableMapOf<Pair<Int, Int>, List<EpgProgram>>()

    fun updateChannels(list: List<EpgChannel>) {
        // Programme are keyed by the channel's transient grid index. A provider sync can
        // insert, remove, or reorder channels, so keeping the old index-keyed values would
        // render one sender's real programme on another sender until the next refresh.
        // Clear them atomically with the channel replacement; refreshPrograms() repopulates
        // the current mapping from Room immediately afterwards.
        programs.clear()
        EPG_CHANNELS.clear()
        EPG_CHANNELS.addAll(list)
    }

    fun setPrograms(vi: Int, day: Int, list: List<EpgProgram>) {
        programs[vi to day] = list
    }

    fun programsFor(vi: Int, day: Int): List<EpgProgram> = programs[vi to day] ?: emptyList()

    fun clear() {
        EPG_CHANNELS.clear()
        programs.clear()
    }
}

/**
 * Real EPG data only. Returns whatever was stored for (channel, day) from the
 * database — never fabricated schedules. A channel without guide data yields an
 * empty list, and the grid shows an honest empty state instead of mock content.
 */
fun epgProgramsFor(vi: Int, day: Int): List<EpgProgram> = EpgStore.programsFor(vi, day)

fun epgProgAt(vi: Int, day: Int, min: Int): Int {
    val a = epgProgramsFor(vi, day)
    var b = 0
    for (idx in a.indices) if (a[idx].s <= min) b = idx
    return b
}

fun nowProg(vi: Int, day: Int = 0, now: Int = nowMin()): EpgProgram? =
    epgProgramsFor(vi, day).getOrNull(epgProgAt(vi, day, now))

// ---------------------------------------------------------------------------
// Provider category groups (Core-TV gate: "all provider categories/groups are
// reachable from Live TV and Guide"). Pure and JVM-testable — the Guide groups
// playlists by the REAL `Channel.category` value the providers returned.

/** Active Guide group state. The settings writers keep the three fields mutually exclusive. */
data class EpgGroupState(
    val favsOnly: Boolean,
    val recentOnly: Boolean,
    val category: String?
)

/**
 * Distinct provider categories in first-seen (playlist) order — matching how Live TV
 * renders rows. Blank/null categories are skipped: channels without a group stay in
 * "all" and are never invented into a fabricated category (masterplan rule #2).
 */
fun epgCategories(channels: List<Channel>): List<String> =
    channels.mapNotNull { it.category?.takeIf { c -> c.isNotBlank() } }.distinct()

/**
 * Global Guide indices of all channels in [category] (exact match on the stored value,
 * consistent with Live TV's category rows). Unknown categories / empty input yield an
 * empty list — never fabricated entries.
 */
fun epgCategoryVis(category: String, channels: List<Channel>): List<Int> =
    channels.indices.filter { channels[it].category == category }

/**
 * Ordered group cycle of the Guide: the virtual groups ("all", "favs", "recent") first,
 * then one entry per provider category. Category ids are prefixed ("cat:X") so a provider
 * category named e.g. "all" can never collide with the virtual groups.
 */
fun epgGroupCycle(categories: List<String>): List<String> =
    listOf("all", "favs", "recent") + categories.map { "cat:$it" }

/**
 * The next group id after [state] in the cycle (wraps to "all"). Empty virtual groups
 * ([includeFavs]/[includeRecent]) are skipped so users never press through empty groups.
 * An active category that vanished from the provider data (renamed/removed) falls back
 * to "all" instead of hanging the cycle.
 */
fun epgNextGroup(
    state: EpgGroupState,
    categories: List<String>,
    includeFavs: Boolean,
    includeRecent: Boolean
): String {
    val cycle = buildList {
        add("all")
        if (includeFavs) add("favs")
        if (includeRecent) add("recent")
        addAll(categories.map { "cat:$it" })
    }
    val current = when {
        state.category != null -> "cat:${state.category}"
        state.favsOnly -> "favs"
        state.recentOnly -> "recent"
        else -> "all"
    }
    val idx = cycle.indexOf(current)
    return cycle[(idx + 1).coerceAtLeast(0) % cycle.size]
}

/** Decodes a cycle id back to the plain category name ("cat:X" -> "X"); other ids unchanged. */
fun epgGroupCategoryName(id: String): String =
    if (id.startsWith("cat:")) id.substring(4) else id

/**
 * Horizontal scroll target (in px) that brings the focused programme into view, with a small
 * leading padding. Pure and JVM-testable so the Guide's focus-follow logic is verifiable
 * without a device:
 * - `prog == null` (channel without EPG data) snaps to the start of the window.
 * - the result is always clamped to `[0, totalWidth - gridWidth]`, so it never over-scrolls.
 */
fun epgScrollTargetX(
    prog: EpgProgram?,
    gridWidthPx: Int,
    timeColWpx: Int,
    padPx: Int = 24
): Int {
    if (gridWidthPx < 0 || timeColWpx <= 0) return 0
    val totalW = timeColWpx * EPG_HOURS
    val maxX = (totalW - gridWidthPx).coerceAtLeast(0)
    val left = if (prog != null) {
        val s = prog.s.coerceAtLeast(EPG_START_MIN)
        ((s - EPG_START_MIN).toFloat() / 60f * timeColWpx).toInt()
    } else 0
    return (left - padPx).coerceIn(0, maxX)
}

fun displayWindowStart(day: Int): Long {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, EPG_START_MIN / 60)
        set(Calendar.MINUTE, EPG_START_MIN % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.DAY_OF_YEAR, day)
    }
    return cal.timeInMillis
}

/**
 * End of the local 05:00–05:00 guide window. This must be calculated as the next local
 * calendar boundary rather than `start + 24h`: a daylight-saving transition can make that
 * interval 23 or 25 elapsed hours.
 */
fun displayWindowEnd(day: Int): Long = displayWindowStart(day + 1)

private fun wallClockSlot(timestamp: Long, start: Long, end: Long): Int {
    if (timestamp <= start) return EPG_START_MIN
    if (timestamp >= end) return EPG_END_MIN
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val minuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    // The displayed day begins at 05:00, so the following local midnight–04:59 segment
    // occupies the tail of the same 24-column grid. This is wall-clock math, deliberately
    // independent of the elapsed milliseconds on DST transition nights.
    return if (minuteOfDay < EPG_START_MIN) minuteOfDay + 24 * 60 else minuteOfDay
}

fun mapDbPrograms(dbPrograms: List<DbEpgProgram>, day: Int): List<EpgProgram> {
    val start = displayWindowStart(day)
    val end = displayWindowEnd(day)
    return dbPrograms.filter { it.startTs < end && it.endTs > start }.map { prog ->
        // Slot minutes are WALL-CLOCK minutes since midnight (the grid header, the now-line,
        // epgProgAt/nowProg and ProgramRow all compare against nowMin()/hh()/EPG_START in that
        // basis). The DB window starts at 05:00, so its next-midnight segment needs the
        // following-day offset (00:00 -> 1440); `wallClockSlot` keeps that basis correct
        // even on daylight-saving transition nights.
        val s = wallClockSlot(prog.startTs, start, end)
        val e = wallClockSlot(prog.endTs, start, end)
        EpgProgram(s = s, e = e, t = prog.title, c = prog.description ?: "")
    }
}

fun mapDbChannel(ch: Channel, index: Int): EpgChannel {
    val colors = channelColors(ch.name)
    return EpgChannel(
        name = ch.name,
        code = channelCode(ch.name),
        colorStart = colors.first,
        colorEnd = colors.second,
        num = if (ch.number > 0) ch.number else index + 1
    )
}

private fun channelCode(name: String): String {
    val cleaned = name.replace(".", " ").replace("-", " ")
    val parts = cleaned.split(" ").filter { it.isNotBlank() }
    return if (parts.size >= 2) {
        parts.take(2).map { it.first().uppercaseChar() }.joinToString("")
    } else {
        name.take(3).uppercase()
    }
}

private fun channelColors(name: String): Pair<Long, Long> {
    val hash = name.fold(0) { acc, c -> acc * 31 + c.code }
    val hue = (hash % 360).let { if (it < 0) it + 360 else it }
    val c1 = hslColor(hue, 0.7f, 0.35f)
    val c2 = hslColor((hue + 30) % 360, 0.8f, 0.55f)
    return c1 to c2
}

private fun hslColor(h: Int, s: Float, l: Float): Long {
    val c = (1f - kotlin.math.abs(2 * l - 1f)) * s
    val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
    val m = l - c / 2f
    val (r1, g1, b1) = when (h) {
        in 0..59 -> Triple(c, x, 0f)
        in 60..119 -> Triple(x, c, 0f)
        in 120..179 -> Triple(0f, c, x)
        in 180..239 -> Triple(0f, x, c)
        in 240..299 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val r = ((r1 + m) * 255).toInt().coerceIn(0, 255)
    val g = ((g1 + m) * 255).toInt().coerceIn(0, 255)
    val b = ((b1 + m) * 255).toInt().coerceIn(0, 255)
    return (0xFF000000L or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong())
}

internal fun pad2(n: Int): String = if (n < 10) "0$n" else "$n"

internal fun hh(min: Int): String {
    val m = ((min % 1440) + 1440) % 1440
    return pad2(m / 60) + ":" + pad2(m % 60)
}

fun fmtDur(min: Int): String {
    var m = min
    if (m <= 0) m = 0
    val h = m / 60
    return (if (h > 0) "$h:" else "") + pad2(m % 60)
}

private fun clamp(v: Int, a: Int, b: Int): Int = Math.max(a, Math.min(b, v))

internal fun clampInt(v: Int, a: Int, b: Int): Int = clamp(v, a, b)

fun nowMin(): Int = LocalTime.now().hour * 60 + LocalTime.now().minute

fun channelColor(vi: Int): Color {
    val ch = EPG_CHANNELS.getOrNull(vi) ?: EPG_CHANNELS.firstOrNull() ?: return Color.Gray
    return Color(ch.colorStart)
}

fun channelArt(vi: Int): List<Color> {
    val ch = EPG_CHANNELS.getOrNull(vi) ?: EPG_CHANNELS.firstOrNull() ?: return listOf(Color.Gray, Color.DarkGray)
    return listOf(Color(ch.colorStart), Color(ch.colorEnd))
}

fun dayLabel(day: Int): String {
    val d = Date(System.currentTimeMillis() + day * 86400_000L)
    return SimpleDateFormat("EE., dd.MM.", Locale.GERMAN).format(d)
}

fun dayToast(day: Int): String = when (day) {
    0 -> "Heute"
    1 -> "Morgen"
    else -> "In $day Tagen"
}

fun programChannelLabel(vi: Int): String {
    val ch = EPG_CHANNELS.getOrNull(vi) ?: EPG_CHANNELS.firstOrNull() ?: return ""
    return ch.name + " · Kanal " + ch.num
}
