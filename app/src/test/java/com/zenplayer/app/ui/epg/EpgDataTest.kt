package com.zenplayer.app.ui.epg

import com.zenplayer.app.data.model.Channel
import com.zenplayer.app.data.model.EpgProgram as DbEpgProgram
import com.zenplayer.app.data.model.MediaType
import java.util.Calendar
import java.util.TimeZone
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
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
    fun updateChannels_clearsIndexBoundPrograms_beforeChannelListIsReplaced() {
        EpgStore.clear()
        EpgStore.setPrograms(0, 0, listOf(EpgProgram(s = 300, e = 360, t = "Sender A", c = "")))

        // Index 0 may point to a different provider channel after a playlist re-sync.
        EpgStore.updateChannels(listOf(EpgChannel("Sender B", "SB", 0L, 1L, 1)))

        assertTrue(epgProgramsFor(0, 0).isEmpty())
    }

    @Test
    fun displayWindowStart_is5amLocalMidnight() {
        val start = displayWindowStart(0)
        val cal = Calendar.getInstance().apply { timeInMillis = start }
        assertEquals(5, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))

        val start1 = displayWindowStart(1)
        val next = Calendar.getInstance().apply { timeInMillis = start1 }
        assertEquals(5, next.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, next.get(Calendar.MINUTE))
        cal.add(Calendar.DAY_OF_YEAR, 1)
        assertEquals(cal.get(Calendar.DAY_OF_YEAR), next.get(Calendar.DAY_OF_YEAR))
    }

    @Test
    fun mapDbPrograms_mapsWallClockMinutesSinceMidnight() {
        // DB window starts at 05:00 (displayWindowStart); a programme that really airs at
        // 12:00 sits 7 h later. Slot minutes are WALL-CLOCK minutes since midnight, so the
        // mapping must add EPG_START_MIN (300) — otherwise 12:00 would map to s=420 and the
        // grid would render every programme five hours too early.
        val start = displayWindowStart(0)
        val noonStart = start + 7 * 60 * 60_000L
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
    fun mapDbPrograms_slotBasisAlignsWithHeaderAndNowLine() {
        // Grid invariants: column h shows hour (h + 5) % 24 and the now-line sits at
        // (nowMin - EPG_START). Slot s must use the SAME basis so the now-column matches.
        // 05:00 → s=300 → column 0 → header hour 05:00 (arrows: (0+5)%24).
        // 12:00 → s=720 → column (720-300)/60 = 7 → header (7+5)%24 = 12:00.
        val start = displayWindowStart(0)
        val prog5 = DbEpgProgram("p1", "ch1", 1L, start, start + 60 * 60_000L, "Früh", null)
        val prog12 = DbEpgProgram("p2", "ch1", 1L, start + 7 * 60 * 60_000L, start + 8 * 60 * 60_000L, "Mittag", null)
        val mapped = mapDbPrograms(listOf(prog5, prog12), 0)

        assertEquals(EPG_START_MIN, mapped[0].s)         // 05:00 → left edge of the grid
        assertEquals(12 * 60, mapped[1].s)               // 12:00 → column 7, header "12:00"
        assertEquals(13 * 60, mapped[1].e)               // 13:00 → column 8, header "13:00"
        // hh(s) must print the real wall-clock start time on the programme card.
        assertEquals("05:00", hh(mapped[0].s))
        assertEquals("12:00", hh(mapped[1].s))
        assertEquals("13:00", hh(mapped[1].e))
    }

    @Test
    fun mapDbPrograms_keepsWallClockSlotsAcrossSpringDstTransition() {
        val originalZone = TimeZone.getDefault()
        val zone = ZoneId.of("Europe/Berlin")
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(zone.id))
            val springTransition = generateSequence(zone.rules.nextTransition(Instant.now())) {
                zone.rules.nextTransition(it.instant.plusSeconds(1))
            }.first { it.offsetAfter.totalSeconds > it.offsetBefore.totalSeconds }
            val transitionDay = springTransition.dateTimeBefore.toLocalDate()
            val day = ChronoUnit.DAYS.between(LocalDate.now(zone), transitionDay.minusDays(1)).toInt()
            val beforeFive = LocalDateTime.of(transitionDay, java.time.LocalTime.of(4, 30))
                .atZone(zone).toInstant().toEpochMilli()
            val mapped = mapDbPrograms(
                listOf(DbEpgProgram("p1", "ch1", 1L, beforeFive, displayWindowEnd(day), "Früh", null)),
                day
            )

            // 04:30 on the following local day is the final half-hour of the 05:00–05:00
            // grid, even though spring DST made its elapsed distance from [start] 23 h 30 m.
            assertEquals(1710, mapped.single().s)
            assertEquals(EPG_END_MIN, mapped.single().e)
        } finally {
            TimeZone.setDefault(originalZone)
        }
    }

    @Test
    fun nowProg_selectsProgramRunningAtGivenNow() {
        EpgStore.clear()
        // Real programme airing 12:00–13:00 (slot s=720, e=780).
        EpgStore.setPrograms(0, 0, listOf(EpgProgram(s = 720, e = 780, t = "Mittagsmagazin", c = "live")))

        // At 12:30 (nowMin = 750) the running programme is the noon one — the slot basis is
        // wall-clock minutes, so s=720 matches nowMin=750 exactly as the player overlay
        // (startTs <= now && endTs > now) would.
        val prog = nowProg(0, 0, 750)
        assertEquals("Mittagsmagazin", prog?.t)
    }

    // --- Early-morning "now" correctness (Core-TV gate: current time / now marker) ---

    @Test
    fun wallClockToSlot_mapsPreDawnToGridTail() {
        // 00:00–04:59 occupy the tail of the 05:00–05:00 grid (1440–1739); the rest maps 1:1.
        assertEquals(1440, wallClockToSlot(0))
        assertEquals(1560, wallClockToSlot(120)) // 02:00 -> column 21
        assertEquals(1739, wallClockToSlot(299)) // 04:59 -> last column
        assertEquals(300, wallClockToSlot(300))  // 05:00 -> left edge
        assertEquals(840, wallClockToSlot(840))  // 14:00 -> column 9
        assertEquals(1439, wallClockToSlot(1439))
    }

    @Test
    fun effectiveDay_shiftsPreDawnToPreviousBroadcastDay() {
        // Before 05:00 the current moment belongs to the previous 05:00–05:00 window.
        assertEquals(-1, effectiveDay(0, 120))   // 02:00 "Heute" -> window [gestern 05:00, heute 05:00)
        assertEquals(0, effectiveDay(0, 300))    // 05:00 exact -> today's window
        assertEquals(0, effectiveDay(0, 1439))   // 23:59 -> today's window
        assertEquals(0, effectiveDay(1, 120))    // "morgen" pre-dawn -> today's window
        assertEquals(5, effectiveDay(6, 120))    // last preference pre-dawn -> day 5 window
    }

    @Test
    fun nowProg_selectsRunningEarlyMorningProgramme() {
        EpgStore.clear()
        // The previous broadcast window (day -1): a programme that started at 00:30 today
        // lives in the TAIL columns (slot 1470 = 00:30 + 1440), not at the grid start.
        EpgStore.setPrograms(0, -1, listOf(EpgProgram(s = 1470, e = 1560, t = "Nachtprogramm", c = "live")))

        // At 02:00 (nowMin = 120) the running programme is the night one. Before the fix
        // nowProg compared 120 against slots >= 300 and fell back to index 0 (05:00).
        val prog = nowProg(0, -1, 120)
        assertEquals("Nachtprogramm", prog?.t)
    }

    @Test
    fun nowProg_earlyMorningDoesNotPickFirstProgram() {
        EpgStore.clear()
        // Mixed day: 05:00 morning show + early-morning tail programme of the window's
        // following day. At 02:00 (now=120) the tail programme must be selected — NOT the
        // first (05:00) entry.
        EpgStore.setPrograms(
            0, 0,
            listOf(
                EpgProgram(s = 300, e = 390, t = "Frühstücksfernsehen", c = "live"),
                EpgProgram(s = 1560, e = 1620, t = "Spätspätprogramm", c = "live")
            )
        )
        assertEquals(1, epgProgAt(0, 0, 120))
        assertEquals("Spätspätprogramm", nowProg(0, 0, 120)?.t)
        // During the day both consumers keep their documented behavior.
        assertEquals("Frühstücksfernsehen", nowProg(0, 0, 330)?.t)
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
