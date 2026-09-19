package com.zenplayer.app.player

import com.zenplayer.app.data.settings.PlayerEngineChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FallbackPolicyTest {

    @Test
    fun exoPreferred_startsExo_andFallsBackToVlcOnce() {
        val policy = FallbackPolicy(PlayerEngineChoice.EXO)
        assertEquals(PlayerEngineChoice.EXO, policy.engineToStart())
        assertTrue(policy.canFallback())

        val fallback = policy.markPrimaryFailed()
        assertEquals(PlayerEngineChoice.VLC, fallback)
        assertFalse(policy.canFallback())
        assertTrue(policy.primaryFailed)
    }

    @Test
    fun vlcPreferred_startsVlc_andFallsBackToExoOnce() {
        val policy = FallbackPolicy(PlayerEngineChoice.VLC)
        assertEquals(PlayerEngineChoice.VLC, policy.engineToStart())

        val fallback = policy.markPrimaryFailed()
        assertEquals(PlayerEngineChoice.EXO, fallback)
        assertFalse(policy.canFallback())
    }

    @Test
    fun afterPrimaryFailure_startsDirectlyOnFallback() {
        val policy = FallbackPolicy(PlayerEngineChoice.EXO)
        policy.markPrimaryFailed()
        assertEquals(PlayerEngineChoice.VLC, policy.engineToStart())
        assertTrue(policy.isFallback(PlayerEngineChoice.VLC))
    }

    @Test
    fun noPingPong_secondFailureCannotFallbackAgain() {
        val policy = FallbackPolicy(PlayerEngineChoice.EXO)
        policy.markPrimaryFailed()
        assertFalse(policy.canFallback())
        // A second failure on the fallback engine must not trigger another switch.
        assertEquals(PlayerEngineChoice.VLC, policy.engineToStart())
    }
}
