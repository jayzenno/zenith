package com.zenplayer.app.ui.home

import com.zenplayer.app.data.model.Channel
import com.zenplayer.app.data.settings.recentOnlyVis

/**
 * Pure, JVM-testable decision logic for the Home top banner (masterplan rule #2:
 * no fabricated content in production). The hero is never an invented "now playing"
 * banner — it always shows a REAL live channel and its REAL current programme.
 *
 * Selection order:
 * 1. the most recently watched live channel (recency order),
 * 2. otherwise the first favorite,
 * 3. otherwise the very first real live channel.
 *
 * Returns null when there is no channel data at all; the Home then renders an honest
 * onboarding/empty state instead of fabricated content.
 */
fun homeHeroIndex(channels: List<Channel>, favs: Set<Int>, recent: List<String>): Int? {
    if (channels.isEmpty()) return null
    val recentIdx = recentOnlyVis(recent, channels).firstOrNull()
    if (recentIdx != null && recentIdx in channels.indices) return recentIdx
    val favIdx = favs.firstOrNull { it in channels.indices }
    return favIdx ?: 0
}