package com.zenplayer.app.data.settings

import com.zenplayer.app.data.model.Channel
import com.zenplayer.app.data.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the pure logic of the "Zuletzt gesehen" (Recently Watched) first-class
 * virtual group (Core-TV gate #8): dedupe/move-to-front/cap semantics, provider-scoped
 * identity, persisted-format round-trip and recency-ordered index mapping.
 */
class RecentWatchTest {

    private fun ch(id: String, providerId: Long = 1L) = Channel(
        id = id,
        providerId = providerId,
        mediaType = MediaType.LIVE,
        name = id,
        url = "http://example.com/$id"
    )

    @Test
    fun pushRecentWatch_movesExistingKeyToFront() {
        assertEquals(
            listOf("b", "a", "c"),
            pushRecentWatch(current = listOf("a", "b", "c"), key = "b")
        )
    }

    @Test
    fun pushRecentWatch_addsNewKeyToFront() {
        assertEquals(
            listOf("d", "a", "b"),
            pushRecentWatch(current = listOf("a", "b"), key = "d")
        )
    }

    @Test
    fun pushRecentWatch_isIdempotentForFrontKey() {
        assertEquals(
            listOf("a", "b"),
            pushRecentWatch(current = listOf("a", "b"), key = "a")
        )
    }

    @Test
    fun pushRecentWatch_capsAtLimit() {
        val capped = pushRecentWatch(
            current = listOf("a", "b", "c", "d"),
            key = "e",
            limit = 3
        )
        assertEquals(listOf("e", "a", "b"), capped)
    }

    @Test
    fun recentWatchKey_isProviderScoped() {
        val a = recentWatchKey(ch("same", providerId = 1L))
        val b = recentWatchKey(ch("same", providerId = 2L))
        assertTrue("gleiche id, andere provider -> andere Keys", a != b)
        assertEquals("2:same", recentWatchKey(ch("same", providerId = 2L)))
    }

    @Test
    fun recentDecodeEncode_roundTrip() {
        assertEquals("a\nb", recentWatchEncode(listOf("a", "b")))
        assertEquals(listOf("a", "b"), recentWatchDecode("a\nb"))
        // Null / leer / nur Zeilenumbrüche -> leere Liste (keine Müll-Einträge).
        assertTrue(recentWatchDecode(null).isEmpty())
        assertTrue(recentWatchDecode("").isEmpty())
        assertTrue(recentWatchDecode("a\n\nb\n").containsAll(listOf("a", "b")))
    }

    @Test
    fun recentOnlyVis_preservesRecencyOrderAndSkipsUnknown() {
        val channels = listOf(ch("a"), ch("b"), ch("c"))
        // recent = most-recent-first; "missing" kennt der aktuelle Kanalbestand nicht.
        assertEquals(listOf(2, 0), recentOnlyVis(listOf("1:c", "1:a", "missing"), channels))
    }

    @Test
    fun recentOnlyVis_emptyForEmptyInputs() {
        assertTrue(recentOnlyVis(recent = emptyList(), channels = listOf(ch("a"))).isEmpty())
        assertTrue(recentOnlyVis(recent = listOf("1:a"), channels = emptyList()).isEmpty())
    }

    @Test
    fun recentOnlyVis_skipsChannelsRemovedFromProvider() {
        // "1:gone" existiert nicht mehr nach einem Re-Sync -> trotzdem gelistet? Nein,
        // es wird übersprungen; die übrigen behalten ihre Recency-Reihenfolge.
        val channels = listOf(ch("a"), ch("b"))
        assertEquals(
            listOf(1, 0),
            recentOnlyVis(listOf("1:b", "1:gone", "1:a"), channels)
        )
    }
}