package com.zenplayer.app.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackStatusTest {

    @Test
    fun progressLive_trueWhileStreamIsAdvancing() {
        assertTrue(PlaybackStatus.Playing.progressLive)
        assertTrue(PlaybackStatus.Buffering.progressLive)
    }

    @Test
    fun progressLive_falseEverywhereElseProgressIsFrozen() {
        // Every non-advancing state must freeze the last known progress instead of
        // polling the engines (see ZenPlayerSession.startTicker).
        assertFalse(PlaybackStatus.Idle.progressLive)
        assertFalse(PlaybackStatus.Loading.progressLive)
        assertFalse(PlaybackStatus.Ready.progressLive)
        assertFalse(PlaybackStatus.Paused.progressLive)
        assertFalse(PlaybackStatus.Ended.progressLive)
        assertFalse(PlaybackStatus.Timeout.progressLive)
        assertFalse(PlaybackStatus.Error("test").progressLive)
    }

    @Test
    fun settledAfterStart_trueForUsableAndTerminalStates() {
        // The watchdog must count Paused as settled: pausing during tune-in (HOME in the
        // start window) must neither trip a bogus timeout nor start a background fallback.
        assertTrue(PlaybackStatus.Playing.settledAfterStart())
        assertTrue(PlaybackStatus.Ready.settledAfterStart())
        assertTrue(PlaybackStatus.Paused.settledAfterStart())
        assertTrue(PlaybackStatus.Error("test").settledAfterStart())
        assertTrue(PlaybackStatus.Ended.settledAfterStart())
    }

    @Test
    fun settledAfterStart_falseForTransientAndUnstartedStates() {
        assertFalse(PlaybackStatus.Idle.settledAfterStart())
        assertFalse(PlaybackStatus.Loading.settledAfterStart())
        assertFalse(PlaybackStatus.Buffering.settledAfterStart())
        assertFalse(PlaybackStatus.Timeout.settledAfterStart())
    }
}