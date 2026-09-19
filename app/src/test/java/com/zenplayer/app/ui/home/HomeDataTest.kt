package com.zenplayer.app.ui.home

import com.zenplayer.app.data.model.Channel
import com.zenplayer.app.data.model.EpgProgram as DbEpgProgram
import com.zenplayer.app.data.model.MediaType
import com.zenplayer.app.data.settings.recentWatchKey
import com.zenplayer.app.ui.epg.EPG_START_MIN
import com.zenplayer.app.ui.epg.displayWindowStart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeDataTest {

    private fun ch(id: String, providerId: Long = 1, extra: String? = null): Channel =
        Channel(
            id = id,
            providerId = providerId,
            mediaType = MediaType.LIVE,
            name = "Kanal $id",
            url = "http://example.com/$id.m3u8",
            extra = extra
        )

    private fun channels(n: Int): List<Channel> = List(n) { ch("c$it") }

    // Timestamp helper: WALL-CLOCK minute-of-day [m] (the slot basis mapDbPrograms uses,
    // 300 = 05:00) translated into millis on today's local 05:00–05:00 window, so fixture
    // times stay valid on any day. Early-morning m < 300 would sit on the NEXT window's
    // tail — tests for that path build their own day=-1 timestamps explicitly.
    private fun ts(m: Int): Long = displayWindowStart(0) + (m - EPG_START_MIN) * 60_000L

    private fun prog(channelId: String, fromMin: Int, toMin: Int, title: String): DbEpgProgram =
        DbEpgProgram(
            id = "$channelId:$title",
            channelId = channelId,
            providerId = 1,
            startTs = ts(fromMin),
            endTs = ts(toMin),
            title = title
        )

    @Test
    fun `empty channel list yields null hero`() {
        assertNull(homeHeroIndex(emptyList(), emptySet(), emptyList()))
        assertNull(homeHeroIndex(emptyList(), setOf(0, 3), listOf("1:c1")))
    }

    @Test
    fun `no favourites and no recency uses first channel`() {
        assertEquals(0, homeHeroIndex(channels(3), emptySet(), emptyList()))
    }

    @Test
    fun `valid favourite is preferred over first channel`() {
        assertEquals(2, homeHeroIndex(channels(5), setOf(2), emptyList()))
    }

    @Test
    fun `out-of-range favourite is ignored`() {
        assertEquals(0, homeHeroIndex(channels(3), setOf(7, 99), emptyList()))
    }

    @Test
    fun `most recently watched channel wins over favourites`() {
        val list = channels(6)
        // The recency list is most-recent-first; index 4 is the newest.
        val recent = listOf(recentWatchKey(list[4]), recentWatchKey(list[1]))
        assertEquals(4, homeHeroIndex(list, setOf(0), recent))
    }

    @Test
    fun `second recent entry is used when first is unavailable`() {
        val list = channels(5)
        // First key points to a channel that no longer exists in the list.
        val recent = listOf("9:ghost", recentWatchKey(list[3]))
        assertEquals(3, homeHeroIndex(list, setOf(1), recent))
    }

    @Test
    fun `recency keys without any matching channel fall back to favourites`() {
        val list = channels(4)
        val recent = listOf("1:ghost", "2:ghost")
        assertEquals(2, homeHeroIndex(list, setOf(2), recent))
    }

    @Test
    fun `all-candidates-missing recency falls back to first channel`() {
        val list = channels(4)
        val recent = listOf("1:ghost")
        assertEquals(0, homeHeroIndex(list, emptySet(), recent))
    }

    @Test
    fun `recent key is provider-scoped so equal ids never collide`() {
        val a = ch("same", providerId = 1)
        val b = ch("same", providerId = 2)
        val list = listOf(a, b)
        val recent = listOf(recentWatchKey(b))
        // Only provider 2's channel matches the key -> index 1 wins.
        assertEquals(1, homeHeroIndex(list, emptySet(), recent))
    }

    @Test
    fun `empty favourites and recency with single channel uses index zero`() {
        assertEquals(0, homeHeroIndex(listOf(ch("only")), setOf(3, 9), listOf("9:ghost")))
    }

    // ----------------------------------------------------------------------
    // homeNowPrograms: Home "Jetzt LIVE" reads REAL Room programme data directly
    // (no Guide in-memory store, no fabricated schedules — masterplan rule #2).

    @Test
    fun `nowPrograms empty when nothing stored`() {
        assertTrue(homeNowPrograms(emptyList(), channels(3), day = 0, now = 540).isEmpty())
    }

    @Test
    fun `nowPrograms empty when no channels`() {
        assertTrue(homeNowPrograms(listOf(prog("c0", 480, 600, "Tagesschau")), emptyList(), day = 0, now = 540).isEmpty())
    }

    @Test
    fun `nowPrograms returns running programme by stream id`() {
        val list = channels(2)
        val db = listOf(prog("c0", 480, 600, "Tagesschau"), prog("c0", 300, 470, "Frühstück"))
        val map = homeNowPrograms(db, list, day = 0, now = 540)
        assertEquals(1, map.size)
        assertEquals("Tagesschau", map[0]?.t)
        // index 1 has no stored programme -> honest absence, never fabricated.
        assertNull(map[1])
    }

    @Test
    fun `nowPrograms matches epg identity via extra not stream id`() {
        // Provider SM removes the tvg id into `extra`; the DB stores that identity.
        val list = listOf(ch(id = "12345", extra = "de.example.tv"))
        val db = listOf(prog("de.example.tv", 480, 600, "Heute Journal"))
        val map = homeNowPrograms(db, list, day = 0, now = 540)
        assertEquals(1, map.size)
        assertEquals("Heute Journal", map[0]?.t)
    }

    @Test
    fun `nowPrograms excludes channels without matching epg identity`() {
        val list = listOf(ch(id = "12345", extra = "de.example.tv"))
        val db = listOf(prog("overlay.de.other", 480, 600, "Fremdsender"))
        assertTrue(homeNowPrograms(db, list, day = 0, now = 540).isEmpty())
    }

    @Test
    fun `nowPrograms picks latest started programme like the guide`() {
        val list = channels(1)
        // Ascending startTs — the real ORDER BY startTs ASC of observeForWindow.
        val db = listOf(
            prog("c0", 480, 600, "Bloch"),
            prog("c0", 540, 570, "Spätausgabe"),
            prog("c0", 600, 900, "Tatort")
        )
        val map = homeNowPrograms(db, list, day = 0, now = 540)
        // last programme with s <= now wins (epgProgAt semantics).
        assertEquals("Spätausgabe", map[0]?.t)
    }

    @Test
    fun `nowPrograms skips future programmes`() {
        val list = channels(1)
        val db = listOf(prog("c0", 600, 900, "morgen"))
        assertTrue(homeNowPrograms(db, list, day = 0, now = 540).isEmpty())
    }

    @Test
    fun `nowPrograms is per-channel map across mixed data`() {
        val list = channels(3)
        val db = listOf(
            prog("c0", 480, 600, "Aktuell eins"),
            prog("c2", 300, 560, "Kanal drei neu")
        )
        val map = homeNowPrograms(db, list, day = 0, now = 540)
        assertEquals(2, map.size)
        assertEquals("Aktuell eins", map[0]?.t)
        assertNull(map[1])
        assertEquals("Kanal drei neu", map[2]?.t)
    }

    @Test
    fun `nowPrograms finds early-morning programme in previous window`() {
        // Before 05:00 the running programme lives in the previous broadcast window:
        // effectiveDay shifts Home onto day -1 exactly like the Guide (R6-DST fix).
        // 23:00 yesterday -> now (02:30) via the tail slots, not the first 05:00 entry.
        val list = channels(1)
        val dayStart = displayWindowStart(-1)
        val nightProg = DbEpgProgram(
            id = "c0:night",
            channelId = "c0",
            providerId = 1,
            startTs = dayStart + 18 * 60_000L, // yesterday 23:00 local
            endTs = dayStart + 21 * 60_000L,   // today 02:00 local
            title = "Nachtschleife"
        )
        val map = homeNowPrograms(listOf(nightProg), list, day = -1, now = 150)
        // 02:30 wall clock (150 min) sits in the 1440+ tail -> the night programme matches.
        assertEquals("Nachtschleife", map[0]?.t)
    }
}