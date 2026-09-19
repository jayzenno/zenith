package com.zenplayer.app.ui.home

import com.zenplayer.app.data.model.Channel
import com.zenplayer.app.data.model.EpgProgram as DbEpgProgram
import com.zenplayer.app.data.settings.recentOnlyVis
import com.zenplayer.app.ui.epg.EpgProgram
import com.zenplayer.app.ui.epg.effectiveDay
import com.zenplayer.app.ui.epg.mapDbPrograms
import com.zenplayer.app.ui.epg.nowMin
import com.zenplayer.app.ui.epg.wallClockToSlot

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

/**
 * Real "now" programme per Home channel index, read directly from Room
 * ([DbEpgProgram], as delivered by [com.zenplayer.app.data.repo.EpgRepository.programsForWindowFlow])
 * instead of the Guide's in-memory store. This is the masterplan's definitive Home EPG
 * source: the Home rows and the hero banner show real provider data the moment an EPG sync
 * has stored it — no prior Guide visit required.
 *
 * Identity matching is exactly `EpgViewModel.refreshPrograms`: a channel's EPG identity is
 * `extra` (tvg-id / epg_channel_id from the real provider) when present, else its stream id.
 * Only channels with actual stored programmes get an entry; channels without data map to
 * nothing (never fabricated — masterplan rule #2). The "now" pick uses the same slot-based
 * comparison as the Guide ([com.zenplayer.app.ui.epg.epgProgAt]): the last programme that
 * already started at or before [now], so 00:00–04:59 early-morning programmes are found on
 * the window's tail slots exactly like the Guide clock does.
 *
 * Pure and JVM-testable.
 */
fun homeNowPrograms(
    dbPrograms: List<DbEpgProgram>,
    channels: List<Channel>,
    day: Int = effectiveDay(0, nowMin()),
    now: Int = nowMin()
): Map<Int, EpgProgram> {
    if (dbPrograms.isEmpty() || channels.isEmpty()) return emptyMap()
    val byEpgId = dbPrograms.groupBy { it.channelId }
    val nowSlot = wallClockToSlot(now)
    return channels.indices.mapNotNull { vi ->
        val ch = channels[vi]
        val epgId = ch.extra?.takeIf { it.isNotBlank() } ?: ch.id
        val progs = byEpgId[epgId] ?: return@mapNotNull null
        // The running programme = the one that started last at or before now. maxByOrNull on
        // the slot basis is order-invariant (a Room ORDER BY can't be relied on here): it
        // matches epgProgAt's "last programme with s <= now" for the Guide, independently of
        // the input list order.
        val slot = mapDbPrograms(progs, day).filter { it.s <= nowSlot }.maxByOrNull { it.s }
        if (slot == null) null else vi to slot
    }.toMap()
}