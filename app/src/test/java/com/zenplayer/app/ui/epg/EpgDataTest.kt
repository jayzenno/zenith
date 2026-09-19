package com.zenplayer.app.ui.epg

import com.zenplayer.app.data.model.Channel
import com.zenplayer.app.data.model.EpgProgram as DbEpgProgram
import com.zenplayer.app.data.model.MediaType
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the Core-TV data-honesty rule for the Guide:
 * `epgProgramsFor`/`nowProg` return REAL stored data only and never fall back
 * to fabricated schedules (production mocks were removed).
 */
class EpgDataTest {

    @Test
    fun epgProgramsFor_isEmpty_whenNothingStored() {
        EpgStore.clear()
        assertTrue("Kein Mock-Fallback — leerer Store liefert leere Liste", epgProgramsFor(0, 0).isEmpty())
        assertTrue(epgProgramsFor(1, 0).isEmpty())
        assertTrue(epgProgramsFor(0, 1).isEmpty())
    }

    @Test
    fun nowProg_isNull_whenNothingStored() {
        EpgStore.clear()
        assertNull(nowProg(0, 0, 720))
    }

    @Test
    fun epgProgramsFor_returnsStoredRealPrograms() {
        EpgStore.clear()
        val real = listOf(EpgProgram(s = 300, e = 360, t = "Tagesschau", c = "live"))
        EpgStore.setPrograms(0, 0, real)

        assertEquals(real, epgProgramsFor(0, 0))
        // Anderweitig (anderer Tag / anderer Sender) bleibt es leer.
        assertTrue(epgProgramsFor(0, 1).isEmpty())
        assertTrue(epgProgramsFor(1, 0).isEmpty())
    }

    @Test
    fun displayWindowStart_is5amLocalMidnight() {
        val start = displayWindowStart(0)
        val cal = Calendar.getInstance().apply { timeInMillis = start }
        assertEquals(5, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))

        val start1 = displayWindowStart(1)
        assertEquals(start + 24 * 60 * 60_000L, start1)
    }

    @Test
    fun mapDbPrograms_mapsWindowRelativeMinutes() {
        val start = displayWindowStart(0)
        val noonStart = start + 12 * 60 * 60_000L
        val noonEnd = noonStart + 60 * 60_000L
        val db = listOf(
            DbEpgProgram("p1", "ch1", 1L, noonStart, noonEnd, "Mittagsmagazin", "beschreibung")
        )
        val mapped = mapDbPrograms(db, 0)
        assertEquals(1, mapped.size)
        assertEquals(12 * 60, mapped[0].s) // 12:00 → 720
        assertEquals(13 * 60, mapped[0].e) // 13:00 → 780
        assertEquals("Mittagsmagazin", mapped[0].t)
    }

    @Test
    fun mapDbPrograms_dropsProgramsOutsideWindow() {
        val start = displayWindowStart(0)
        val before = DbEpgProgram("p1", "ch1", 1L, start - 3600_000L, start - 60_000L, "davor", null)
        val after = DbEpgProgram("p2", "ch1", 1L, start + 26 * 60 * 60_000L, start + 27 * 60 * 60_000L, "danach", null)
        assertTrue(mapDbPrograms(listOf(before, after), 0).isEmpty())
    }

    @Test
    fun mapDbChannel_keepsRealNumberAndName() {
        val ch = Channel(
            id = "x1",
            providerId = 1L,
            mediaType = MediaType.LIVE,
            name = "Mein Test Sender",
            url = "http://example.com/live",
            number = 42
        )
        val mapped = mapDbChannel(ch, 5)
        assertEquals("Mein Test Sender", mapped.name)
        assertEquals(42, mapped.num)
        assertEquals("MT", mapped.code)
        assertTrue(mapped.colorStart != mapped.colorEnd)
    }

    @Test
    fun dayStepWindow_shiftsBy24h() {
        val start0 = displayWindowStart(0)
        val cal = Calendar.getInstance().apply { timeInMillis = start0 }
        assertEquals(5, cal.get(Calendar.HOUR_OF_DAY))
    }

    // --- Focus-follow scroll math (virtualized grid, no hardcoded channel cap) ---

    @Test
    fun epgScrollTargetX_clampsToValidRange() {
        // Grid wider than the 24 h timeline -> no scrolling possible at all.
        assertEquals(0, epgScrollTargetX(null, gridWidthPx = 500, timeColWpx = 10))
        // prog at 12:00 (s = 720): left = (720-300)/60*10 = 70px, minus 24px padding.
        val noon = EpgProgram(s = 720, e = 780, t = "Mittag", c = "live")
        assertEquals(46, epgScrollTargetX(noon, gridWidthPx = 100, timeColWpx = 10))
    }

    @Test
    fun epgScrollTargetX_neverOverscrollsPastTimelineEnd() {
        // Last slot before the window end (s = 1700): left = (1700-300)/60*10 = 233px.
        // totalW = 10 * 24 = 240px; with a 100px viewport the max offset is 140px.
        val late = EpgProgram(s = 1700, e = 1740, t = "Spät", c = "live")
        assertEquals(140, epgScrollTargetX(late, gridWidthPx = 100, timeColWpx = 10))
    }

    @Test
    fun epgScrollTargetX_snapToWindowStart_whenNoProgram() {
        assertEquals(0, epgScrollTargetX(null, gridWidthPx = 100, timeColWpx = 10))
        // padPx is not applied when it would push before the window.
        assertEquals(0, epgScrollTargetX(null, gridWidthPx = 100, timeColWpx = 10, padPx = 60))
    }

    // --- Provider category groups (Core-TV gate: categories reachable from the Guide) ---

    private fun ch(id: String, category: String?): Channel = Channel(
        id = id,
        providerId = 1L,
        mediaType = MediaType.LIVE,
        name = id,
        url = "http://example.com/$id",
        category = category
    )

    @Test
    fun epgCategories_returnsDistinctNonBlankInPlaylistOrder() {
        val list = listOf(ch("a", "Sport"), ch("b", "News"), ch("c", "Sport"), ch("d", null), ch("e", "  "), ch("f", "Film"))
        assertEquals(listOf("Sport", "News", "Film"), epgCategories(list))
        // Blank/null categories are never invented into a fabricated group.
        assertTrue(epgCategories(listOf(ch("a", null), ch("b", " "))).isEmpty())
        assertTrue(epgCategories(emptyList()).isEmpty())
    }

    @Test
    fun epgCategoryVis_matchesExactCategoryIndices() {
        val list = listOf(ch("a", "Sport"), ch("b", "News"), ch("c", "Sport"))
        assertEquals(listOf(0, 2), epgCategoryVis("Sport", list))
        assertEquals(listOf(1), epgCategoryVis("News", list))
        assertTrue(epgCategoryVis("Film", list).isEmpty())
        assertTrue(epgCategoryVis("Sport", emptyList()).isEmpty())
    }

    @Test
    fun epgGroupCycle_virtualGroupsThenCategories() {
        assertEquals(listOf("all", "favs", "recent"), epgGroupCycle(emptyList()))
        assertEquals(listOf("all", "favs", "recent", "cat:Sport", "cat:News"), epgGroupCycle(listOf("Sport", "News")))
    }

    @Test
    fun epgNextGroup_cyclesAllFavsRecentThenEachCategoryAndWraps() {
        val cats = listOf("Sport", "News")
        assertEquals("favs", epgNextGroup(EpgGroupState(false, false, null), cats, includeFavs = true, includeRecent = true))
        assertEquals("recent", epgNextGroup(EpgGroupState(true, false, null), cats, includeFavs = true, includeRecent = true))
        assertEquals("cat:Sport", epgNextGroup(EpgGroupState(false, true, null), cats, includeFavs = true, includeRecent = true))
        assertEquals("cat:News", epgNextGroup(EpgGroupState(false, false, "Sport"), cats, includeFavs = true, includeRecent = true))
        // Last category wraps back to "all".
        assertEquals("all", epgNextGroup(EpgGroupState(false, false, "News"), cats, includeFavs = true, includeRecent = true))
    }

    @Test
    fun epgNextGroup_skipsEmptyVirtualGroups() {
        val cats = listOf("Sport")
        // Fresh install: no favorites, no history -> the cycle goes straight to the categories.
        assertEquals("cat:Sport", epgNextGroup(EpgGroupState(false, false, null), cats, includeFavs = false, includeRecent = false))
        assertEquals("all", epgNextGroup(EpgGroupState(false, false, null), emptyList(), includeFavs = false, includeRecent = false))
        // A stale active "favs" flag (favorites deleted while the group was active) falls back to "all".
        assertEquals("all", epgNextGroup(EpgGroupState(true, false, null), listOf("Sport"), includeFavs = false, includeRecent = true))
    }

    @Test
    fun epgNextGroup_unknownCategoryFallsBackToAll() {
        // An active category that vanished from the provider data must not hang the cycle.
        assertEquals("all", epgNextGroup(EpgGroupState(false, false, "Gone"), listOf("Sport", "News"), includeFavs = true, includeRecent = true))
    }

    @Test
    fun epgGroupCategoryName_roundtrips() {
        assertEquals("Sport", epgGroupCategoryName("cat:Sport"))
        assertEquals("all", epgGroupCategoryName("all"))
        // Category names containing the prefix itself still round-trip.
        assertEquals("cat:news", epgGroupCategoryName("cat:cat:news"))
    }
}