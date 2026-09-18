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
 * The EPG grid is not virtualized, so we only ever show a bounded number of channels.
 * Without this cap a real IPTV playlist (thousands of channels) would compose hundreds of
 * thousands of nodes and ANR/OOM the app.
 */
const val MAX_EPG_CHANNELS = 200

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

val EPG_CHANNELS = mutableStateListOf(
    EpgChannel("Das Erste", "DAS", 0xFF24437A, 0xFF3E6CB5, 1),
    EpgChannel("ZDF", "ZDF", 0xFF001D45, 0xFF335D9C, 2),
    EpgChannel("RTL", "RTL", 0xFF2A6AE0, 0xFF18A0FB, 3),
    EpgChannel("Sat.1", "SAT1", 0xFF2A2A3D, 0xFF4C4C6E, 4),
    EpgChannel("ProSieben", "P7", 0xFFE33E5E, 0xFF7D2A52, 5),
    EpgChannel("VOX", "VOX", 0xFFE8A800, 0xFF7A5200, 6),
    EpgChannel("Kabel Eins", "K1", 0xFFD92D27, 0xFF7A1A17, 7),
    EpgChannel("RTLZWEI", "R2", 0xFF7A2A9C, 0xFFC04DFF, 8),
    EpgChannel("ZDFneo", "NEO", 0xFF101426, 0xFF3A4A8C, 9),
    EpgChannel("ARTE", "ARTE", 0xFF2A7B2F, 0xFF4CAF50, 10),
    EpgChannel("one", "1", 0xFF0E2B52, 0xFF1E5AA8, 11),
    EpgChannel("3sat", "3SAT", 0xFF0B3B48, 0xFF159BA8, 12),
    EpgChannel("WDR", "WDR", 0xFF1C4D8C, 0xFF3F7FD6, 13),
    EpgChannel("NDR", "NDR", 0xFF0C4D3F, 0xFF148A6B, 14),
    EpgChannel("SWR", "SWR", 0xFF7A0C0C, 0xFFC41C1C, 15),
    EpgChannel("MDR", "MDR", 0xFF0A3D6B, 0xFF1274B8, 16),
    EpgChannel("Sky", "SKY", 0xFF0B1C3A, 0xFF1E4B9C, 17),
    EpgChannel("Sky Sport", "S-S", 0xFF083344, 0xFF0D7D95, 18),
    EpgChannel("Sky Cinema", "CIN", 0xFF3A0C5E, 0xFF7A2EC4, 19),
    EpgChannel("Sport1", "SP1", 0xFF243645, 0xFF4A7A8C, 20),
    EpgChannel("Eurosport", "EU", 0xFFCF3C3C, 0xFF6B1E1E, 21),
    EpgChannel("DMAX", "MAX", 0xFF0B2418, 0xFF2A6B45, 22),
    EpgChannel("n-tv", "NTV", 0xFF1C2A6E, 0xFF2E4BD6, 23),
    EpgChannel("WELT", "WELT", 0xFF111722, 0xFF3B4A63, 24),
    EpgChannel("Nickelodeon", "NICK", 0xFFD97B00, 0xFFF5A623, 25),
    EpgChannel("Disney Channel", "DIS", 0xFF12317A, 0xFF2C58C4, 26)
)

object EpgStore {
    private val programs = mutableMapOf<Pair<Int, Int>, List<EpgProgram>>()

    fun updateChannels(list: List<EpgChannel>) {
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

fun epgProgramsFor(vi: Int, day: Int): List<EpgProgram> {
    val stored = EpgStore.programsFor(vi, day)
    if (stored.isNotEmpty()) return stored
    return mockProgramsFor(vi, day)
}

fun epgProgAt(vi: Int, day: Int, min: Int): Int {
    val a = epgProgramsFor(vi, day)
    var b = 0
    for (idx in a.indices) if (a[idx].s <= min) b = idx
    return b
}

fun nowProg(vi: Int, day: Int = 0, now: Int = nowMin()): EpgProgram? =
    epgProgramsFor(vi, day).getOrNull(epgProgAt(vi, day, now))

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

fun mapDbPrograms(dbPrograms: List<DbEpgProgram>, day: Int): List<EpgProgram> {
    val start = displayWindowStart(day)
    val end = start + (EPG_END_MIN - EPG_START_MIN) * 60_000L
    return dbPrograms.filter { it.startTs < end && it.endTs > start }.map { prog ->
        val s = ((prog.startTs - start) / 60_000).toInt().coerceIn(EPG_START_MIN, EPG_END_MIN)
        val e = ((prog.endTs - start) / 60_000).toInt().coerceIn(EPG_START_MIN, EPG_END_MIN)
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

private val CHCATS = listOf(
    "news", "news", "news", "news", "news", "show", "doku", "serie", "doku", "doku",
    "news", "doku", "news", "news", "news", "news", "news", "sport", "film", "sport",
    "sport", "doku", "news", "news", "kids", "kids"
)

private val POOL = listOfNotNull(
    listOf("Tagesschau", "news"), listOf("heute", "news"), listOf("heute journal", "news"),
    listOf("WELT Nachrichten", "news"), listOf("n-tv Nachrichten", "news"), listOf("Tagesthemen", "news"),
    listOf("zdf morgenmagazin", "news"), listOf("Sat.1 Frühstücksfernsehen", "news"), listOf("Börse im Blick", "news"),
    listOf("Wetter aktuell", "news"), listOf("Sport1 News", "news"), listOf("Sky News", "news"),
    listOf("Tatort", "serie"), listOf("Polizeiruf 110", "serie"), listOf("Mord mit Aussicht", "serie"),
    listOf("Wilsberg", "serie"), listOf("Nord bei Nordwest", "serie"), listOf("Die Chefin", "serie"),
    listOf("SOKO Leipzig", "serie"), listOf("SOKO Köln", "serie"), listOf("Notruf Hafenkante", "serie"),
    listOf("Großstadtrevier", "serie"), listOf("The Big Bang Theory", "serie"), listOf("Two and a Half Men", "serie"),
    listOf("NCIS", "serie"), listOf("Criminal Minds", "serie"), listOf("CSI: Den Tätern auf der Spur", "serie"),
    listOf("Grey's Anatomy", "serie"), listOf("The Walking Dead", "serie"), listOf("Raumschiff Enterprise", "serie"),
    listOf("Star Trek: Picard", "serie"), listOf("Bergretter", "serie"), listOf("Der Staatsanwalt", "serie"),
    listOf("Die Doku: Dom-Rebellen", "doku"), listOf("Doku: Planet Erde", "doku"), listOf("Doku: Unsere Ozeane", "doku"),
    listOf("Spiegel TV", "doku"), listOf("hart aber fair", "doku"), listOf("Anne Will", "doku"),
    listOf("ZDF Magazin Royale", "doku"), listOf("Die Anstalt", "doku"), listOf("Extra 3", "doku"),
    listOf("Mythen & Monster", "doku"), listOf("How It's Made", "doku"), listOf("Storm Chasers", "doku"),
    listOf("Trödeltrupp", "doku"), listOf("Bares für Rares", "show"), listOf("Das perfekte Dinner", "show"),
    listOf("First Dates – ein Date, das zählt", "show"), listOf("Mein Lokal, Dein Lokal", "show"),
    listOf("Hochzeit auf den ersten Blick", "show"), listOf("Shopping Queen", "show"), listOf("Der Blaulicht Report", "show"),
    listOf("Auf Streife", "show"), listOf("K11 – Die neuen Fälle", "show"), listOf("Ab ins Beet!", "show"),
    listOf("Nur die Liebe zählt", "show"), listOf("ZDFzeit", "doku"), listOf("nano", "news"),
    listOf("arte Journal", "news"), listOf("Wissenschaft: Quantenwelten", "doku"),
    listOf("Star Wars: Die letzte Jedi – Docu", "film"), listOf("Top Gun: Maverick", "film"),
    listOf("Dune: Teil Zwei", "film"), listOf("Oppenheimer", "film"), listOf("Der Pate – Teil II", "film"),
    listOf("Mad Max: Fury Road", "film"), listOf("Inception", "film"), listOf("Interstellar", "film"),
    listOf("Ziemlich beste Freunde", "film"), listOf("Der Schuh des Manitu", "film"),
    listOf("UEFA Europa League", "sport"), listOf("Bundesliga: Topspiel", "sport"),
    listOf("Sky Sport Bundesliga Live", "sport"), listOf("Tennis: Grand Slam Halbfinale", "sport"),
    listOf("Boxen: Heavyweight Night", "sport"), listOf("Wrestling: Royal Rumble", "sport"),
    listOf("SpongeBob Schwammkopf", "kids"), listOf("Phineas und Ferb", "kids"),
    listOf("Disneys Sofia", "kids"), listOf("Die Gummibärenbande", "kids"),
    listOf("Alvin und die Chipmunks", "kids"), listOf("Paw Patrol", "kids"),
    listOf("Bibi Blocksberg", "kids"), listOf("Pumuckl", "kids")
)

private val POOL_CATS = POOL.map { it[1] }

val PROGRAM_CATEGORIES = mutableSetOf<String>().apply { addAll(POOL_CATS) }

private fun progressCategory(cat: String): String = when (cat) {
    "news" -> "Nachrichten"
    "doku" -> "Dokumentation"
    "serie" -> "Serie"
    "show" -> "Show"
    "film" -> "Spielfilm"
    "sport" -> "Sport"
    "kids" -> "Kindersendung"
    else -> "Sendung"
}

private val MOCK_PROGS = HashMap<String, List<EpgProgram>>()

private fun mockProgramsFor(vi: Int, day: Int): List<EpgProgram> {
    if (vi !in EPG_CHANNELS.indices) return emptyList()
    val key = "${vi}_$day"
    MOCK_PROGS[key]?.let { return it }
    val rnd = seedRand(vi * 7919 + day * 104729 + 77)
    val cat = CHCATS[vi % CHCATS.size]
    val pool = POOL.filter { it[1] == cat || rnd() < 0.16 }
    if (pool.isEmpty()) return emptyList()
    val durs = listOf(15, 20, 25, 30, 30, 35, 40, 45, 45, 50, 60, 60, 75, 90, 105, 120, 15, 30, 45)
    val out = mutableListOf<EpgProgram>()
    var m = EPG_START_MIN
    val end = EPG_START_MIN + 1440
    var last = (Math.floor(rnd() * pool.size)).toInt()
    while (m < end) {
        var dr = durs[Math.floor(rnd() * durs.size).toInt()]
        if (m + dr > end) dr = end - m
        if (dr < 10) {
            m += dr
            continue
        }
        var pick = last
        for (x in 0 until 8) {
            val j = (Math.floor(rnd() * pool.size)).toInt()
            if (rnd() < 0.5) pick = j
        }
        out.add(
            EpgProgram(
                s = m,
                e = m + dr,
                t = pool[pick % pool.size][0],
                c = progressCategory(pool[pick % pool.size][1])
            )
        )
        last = pick
        m += dr
    }
    MOCK_PROGS[key] = out
    return out
}

private fun seedRand(seed: Int): () -> Double = {
    var s = (seed * 1664525 + 1013904223) and 0x7FFFFFFF
    rndFromLong(s)
}

private fun rndFromLong(seedVal: Int): Double = seedVal / 2147483648.0
