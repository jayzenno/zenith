package com.zenplayer.app.ui.player

/**
 * Honest quality hint for a live channel, derived from REAL channel data only
 * (the provider's channel name and stream URL). Used for the zap overlay's
 * "quality" badge and wherever a channel card wants a quality marker.
 *
 * Returns one of "4K", "FHD", "HD", "SD" when the provider data explicitly
 * indicates a quality class, otherwise null — unknown quality is never invented
 * (masterplan rule #2: no fabricated metadata in production).
 *
 * Pure and JVM-testable; no Android dependencies.
 */
fun channelQualityHint(name: String, url: String = ""): String? {
    // Underscores are word characters, so "stream_1080p" would defeat \b word
    // boundaries — normalize them to spaces (common in real M3U/Xtream stream URLs).
    val hay = (name + " " + url).lowercase().replace('_', ' ')
    // Order matters: "Ultra HD" and "4K Ultra HD" must resolve to 4K, not HD.
    return when {
        Regex("""\b(4k|uhd|2160p?)\b|ultra\s*hd""").containsMatchIn(hay) -> "4K"
        Regex("""\b(fhd|1080p?)\b""").containsMatchIn(hay) -> "FHD"
        Regex("""\b(hd|720p?)\b""").containsMatchIn(hay) -> "HD"
        Regex("""\b(sd|576p?|480p?)\b""").containsMatchIn(hay) -> "SD"
        else -> null
    }
}