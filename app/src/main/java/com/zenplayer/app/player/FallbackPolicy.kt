package com.zenplayer.app.player

import com.zenplayer.app.data.settings.PlayerEngineChoice

/**
 * Decides which engine to use and whether a single automatic fallback is still allowed.
 *
 * Rules:
 *  - The preferred engine from the settings is always tried first.
 *  - Exactly one automatic fallback to the other engine per session.
 *  - Once the primary failed, later stream starts go straight to the fallback engine so a
 *    device with a broken primary does not pay the start timeout on every zap.
 *  - No ping-pong: after the fallback there is no second automatic switch.
 */
class FallbackPolicy(private val preferred: PlayerEngineChoice) {

    var primaryFailed: Boolean = false
        private set

    /** Engine that should be used for the next [ZenPlayerSession.play] call. */
    fun engineToStart(): PlayerEngineChoice =
        if (primaryFailed) other(preferred) else preferred

    /** True while a single automatic fallback attempt is still available. */
    fun canFallback(): Boolean = !primaryFailed

    /** Records that the primary engine failed and returns the fallback engine. */
    fun markPrimaryFailed(): PlayerEngineChoice {
        primaryFailed = true
        return other(preferred)
    }

    fun isFallback(engine: PlayerEngineChoice): Boolean = engine == other(preferred)

    companion object {
        fun other(engine: PlayerEngineChoice): PlayerEngineChoice =
            if (engine == PlayerEngineChoice.EXO) PlayerEngineChoice.VLC else PlayerEngineChoice.EXO
    }
}
