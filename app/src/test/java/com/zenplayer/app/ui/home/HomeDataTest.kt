package com.zenplayer.app.ui.home

import com.zenplayer.app.data.model.Channel
import com.zenplayer.app.data.model.MediaType
import com.zenplayer.app.data.settings.recentWatchKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeDataTest {

    private fun ch(id: String, providerId: Long = 1): Channel =
        Channel(
            id = id,
            providerId = providerId,
            mediaType = MediaType.LIVE,
            name = "Kanal $id",
            url = "http://example.com/$id.m3u8"
        )

    private fun channels(n: Int): List<Channel> = List(n) { ch("c$it") }

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
}