package com.zenplayer.app.player

/**
 * Defaults that are shared by the player engines. Kept free of Android imports so the
 * values can be unit tested and so a future settings UI only has to change the value
 * handed to [ZenPlayerSession].
 */
object PlayerDefaults {
    const val USER_AGENT = "ZenPlayer/0.1 (Android TV)"
    const val START_TIMEOUT_MS = 12_000L
    const val CONNECT_TIMEOUT_MS = 15_000
    const val READ_TIMEOUT_MS = 20_000
}

enum class StreamType { HLS, DASH, SMOOTH_STREAMING, RTSP, MPEG_TS, PROGRESSIVE, UNKNOWN }

/**
 * Pure helpers used for diagnostics. They never return credentials or full paths:
 * anything derived from a stream URL is reduced to `scheme://host[:port]`.
 */
object PlayerDiagnostics {

    private val URL_REGEX = Regex("""(?i)\b[a-z][a-z0-9+.\-]*://[^\s"'<>]+""")

    /**
     * Returns only `scheme://host[:port]` for a URL. User info, path, query and fragment are
     * dropped so IPTV credentials (often embedded in the path, e.g. Xtream `.../user/pass/id.ts`)
     * can never reach the log.
     */
    fun hostOf(raw: String?): String {
        if (raw.isNullOrBlank()) return "<none>"
        val uri = runCatching { java.net.URI(raw) }.getOrNull() ?: return "<unparsable>"
        val scheme = uri.scheme ?: "?"
        val host = uri.host ?: uri.authority?.substringAfter('@')?.substringBefore(':') ?: "?"
        val port = if (uri.port > 0) ":${uri.port}" else ""
        return "$scheme://$host$port"
    }

    /**
     * Best-effort stream type detection from the URL. Content-Type based detection happens
     * later inside the player; this is only used for logging and diagnostics.
     */
    fun detectStreamType(raw: String?): StreamType {
        if (raw.isNullOrBlank()) return StreamType.UNKNOWN
        val path = raw.substringBefore('?').substringBefore('#').lowercase()
        return when {
            path.startsWith("rtsp://") -> StreamType.RTSP
            path.endsWith(".m3u8") -> StreamType.HLS
            path.endsWith(".mpd") -> StreamType.DASH
            path.endsWith(".ism") || path.contains(".ism/manifest") -> StreamType.SMOOTH_STREAMING
            path.endsWith(".ts") || path.endsWith(".mts") || path.endsWith(".m2ts") -> StreamType.MPEG_TS
            path.endsWith(".mp4") || path.endsWith(".mkv") || path.endsWith(".avi") ||
                path.endsWith(".mov") || path.endsWith(".webm") -> StreamType.PROGRESSIVE
            else -> StreamType.UNKNOWN
        }
    }

    /**
     * Replaces every URL token inside a free-form message with its redacted host. Used for
     * player error messages, which frequently embed the full request URL.
     */
    fun sanitizeMessage(message: String?): String {
        if (message.isNullOrBlank()) return ""
        return URL_REGEX.replace(message) { match -> hostOf(match.value) }
    }
}
