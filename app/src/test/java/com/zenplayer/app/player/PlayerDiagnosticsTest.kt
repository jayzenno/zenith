package com.zenplayer.app.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerDiagnosticsTest {

    @Test
    fun hostOf_redactsCredentialsInPath() {
        val url = "http://iptv.example.com:8080/live/alice/secretpass/12345.ts"
        assertEquals("http://iptv.example.com:8080", PlayerDiagnostics.hostOf(url))
    }

    @Test
    fun hostOf_redactsCredentialsInQuery() {
        val url = "https://example.com/stream?user=alice&pass=secret"
        assertEquals("https://example.com", PlayerDiagnostics.hostOf(url))
    }

    @Test
    fun hostOf_handlesMissingAndUnparsable() {
        assertEquals("<none>", PlayerDiagnostics.hostOf(null))
        assertEquals("<none>", PlayerDiagnostics.hostOf("   "))
        assertEquals("<unparsable>", PlayerDiagnostics.hostOf("not a url"))
    }

    @Test
    fun detectStreamType_byExtension() {
        assertEquals(StreamType.HLS, PlayerDiagnostics.detectStreamType("http://h/a/b.m3u8?token=1"))
        assertEquals(StreamType.DASH, PlayerDiagnostics.detectStreamType("http://h/a/b.mpd"))
        assertEquals(StreamType.MPEG_TS, PlayerDiagnostics.detectStreamType("http://h/live/u/p/1.ts"))
        assertEquals(StreamType.PROGRESSIVE, PlayerDiagnostics.detectStreamType("http://h/movie.mp4"))
        assertEquals(StreamType.RTSP, PlayerDiagnostics.detectStreamType("rtsp://h/stream"))
        assertEquals(StreamType.UNKNOWN, PlayerDiagnostics.detectStreamType("http://h/live/u/p/1"))
        assertEquals(StreamType.UNKNOWN, PlayerDiagnostics.detectStreamType(null))
    }

    @Test
    fun sanitizeMessage_removesCredentialsFromErrorText() {
        val raw = "Unable to connect to http://iptv.example.com:8080/live/alice/secretpass/9.ts"
        val clean = PlayerDiagnostics.sanitizeMessage(raw)
        assertTrue(clean.contains("http://iptv.example.com:8080"))
        assertTrue(!clean.contains("secretpass"))
        assertTrue(!clean.contains("/live/alice"))
    }

    @Test
    fun sanitizeMessage_handlesNullAndPlainText() {
        assertEquals("", PlayerDiagnostics.sanitizeMessage(null))
        assertEquals("plain error", PlayerDiagnostics.sanitizeMessage("plain error"))
    }
}
