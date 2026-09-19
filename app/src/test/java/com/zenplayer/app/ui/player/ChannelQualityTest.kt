package com.zenplayer.app.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChannelQualityTest {

    @Test
    fun `name with explicit 4k resolves to 4K`() {
        assertEquals("4K", channelQualityHint("Das Erste 4K"))
    }

    @Test
    fun `UHD and Ultra HD resolve to 4K, not HD`() {
        assertEquals("4K", channelQualityHint("Sky UHD"))
        assertEquals("4K", channelQualityHint("Ultra HD Kanal"))
        assertEquals("4K", channelQualityHint("4K Ultra HD"))
    }

    @Test
    fun `2160 in stream url resolves to 4K`() {
        assertEquals("4K", channelQualityHint("Mein Kanal", "http://cdn.example/live/2160p.m3u8"))
    }

    @Test
    fun `FHD and 1080 resolve to FHD`() {
        assertEquals("FHD", channelQualityHint("TNT Serie FHD"))
        assertEquals("FHD", channelQualityHint("Movie Channel", "https://example/stream_1080p/index.m3u8"))
    }

    @Test
    fun `HD and 720 resolve to HD`() {
        assertEquals("HD", channelQualityHint("RTL HD"))
        assertEquals("HD", channelQualityHint("Sport1 HD"))
        assertEquals("HD", channelQualityHint("NDR", "https://example/live/720.ts"))
    }

    @Test
    fun `SD markers resolve to SD`() {
        assertEquals("SD", channelQualityHint("Kanal SD"))
        assertEquals("SD", channelQualityHint("Retro TV", "https://example/576p.m3u8"))
    }

    @Test
    fun `case and spacing variations are normalized`() {
        assertEquals("HD", channelQualityHint("  rtl   hd "))
        assertEquals("FHD", channelQualityHint("ZDF FHD"))
    }

    @Test
    fun `quality class inside a longer word is not a hint`() {
        // "hd" inside "Derbys" / "hdmi" must not match — word boundaries only.
        assertNull(channelQualityHint("Derbys HDKabel"))
        assertNull(channelQualityHint("Channel HDMI"))
    }

    @Test
    fun `no quality indicator yields null - never invented`() {
        assertNull(channelQualityHint("NDR"))
        assertNull(channelQualityHint("Das Erste"))
        assertNull(channelQualityHint("", ""))
    }

    @Test
    fun `quality hint wins over sd markers in puffed names`() {
        // "4K SD"? Contradictory provider data: highest explicit class wins.
        assertEquals("4K", channelQualityHint("Demo 4K SD"))
    }
}