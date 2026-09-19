package com.zenplayer.app.data.settings

import com.zenplayer.app.data.model.Channel

/**
 * Pure, JVM-testable logic for the "Zuletzt gesehen" (Recently Watched) first-class
 * virtual group (Core-TV gate "Favorites, All Channels and Recently Watched exist as
 * first-class virtual groups").
 *
 * Persistence format is deliberately trivial (newline-joined keys in DataStore); the
 * dedupe / move-to-front / cap / index-mapping logic lives here in pure functions so
 * it can be verified without a device.
 */

/** Maximum number of channels kept in the Recently Watched group. */
const val RECENT_WATCH_LIMIT = 24

/**
 * Stable identity of a channel for recency tracking. Scoped by provider id so the same
 * URL/id from two providers never collides. Re-syncing a provider keeps the key stable
 * (M3U ids are derived from provider+url+name, Xtream/Stalker ids are server-provided).
 */
fun recentWatchKey(channel: Channel): String = "${channel.providerId}:${channel.id}"

/** Decodes the persisted newline-joined list. `null` / blank input yields an empty list. */
fun recentWatchDecode(raw: String?): List<String> =
    raw?.split('\n')?.filter { it.isNotBlank() } ?: emptyList()

/** Encodes a recency list for persistence (deduplicated, most-recent-first). */
fun recentWatchEncode(list: List<String>): String = list.joinToString("\n")

/**
 * Moves [key] to the front of the recency list, removing any previous occurrence and
 * capping the result at [limit]. Pure and idempotent: pushing an already front-most
 * key returns the list unchanged (apart from the cap).
 */
fun pushRecentWatch(
    current: List<String>,
    key: String,
    limit: Int = RECENT_WATCH_LIMIT
): List<String> = (listOf(key) + current.filterNot { it == key }).take(limit)

/**
 * Maps a persisted recency list onto global Guide indices (vi) for [channels].
 * Unknown/removed channels are skipped; the result keeps recency order, so the most
 * recently watched channel is at index 0 of the virtual group. Empty inputs yield an
 * empty list (never fabricated entries — the Guide shows an honest empty state).
 */
fun recentOnlyVis(recent: List<String>, channels: List<Channel>): List<Int> {
    if (recent.isEmpty() || channels.isEmpty()) return emptyList()
    val indexByKey = HashMap<String, Int>(channels.size)
    channels.forEachIndexed { i, ch -> indexByKey[recentWatchKey(ch)] = i }
    return recent.mapNotNull { indexByKey[it] }
}