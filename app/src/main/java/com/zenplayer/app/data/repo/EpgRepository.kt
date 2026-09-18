package com.zenplayer.app.data.repo

import com.zenplayer.app.data.db.EpgDao
import com.zenplayer.app.data.db.EpgProgramEntity
import com.zenplayer.app.data.model.EpgProgram
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EpgRepository(private val dao: EpgDao) {

    fun upcoming(channelId: String): Flow<List<EpgProgram>> =
        dao.observeUpcoming(channelId, System.currentTimeMillis())
            .map { list -> list.map { it.toModel() } }

    suspend fun programsForDay(channelId: String, day: Int): List<EpgProgram> {
        val start = com.zenplayer.app.ui.epg.displayWindowStart(day)
        val end = start + (com.zenplayer.app.ui.epg.EPG_END_MIN - com.zenplayer.app.ui.epg.EPG_START_MIN) * 60_000L
        return dao.getForChannelAndWindow(channelId, start, end).map { it.toModel() }
    }

    suspend fun programsForWindow(day: Int): List<EpgProgram> {
        val start = com.zenplayer.app.ui.epg.displayWindowStart(day)
        val end = start + (com.zenplayer.app.ui.epg.EPG_END_MIN - com.zenplayer.app.ui.epg.EPG_START_MIN) * 60_000L
        return dao.getForWindow(start, end).map { it.toModel() }
    }

    suspend fun store(programs: List<EpgProgram>) {
        if (programs.isEmpty()) return
        dao.upsertAll(programs.map(EpgProgramEntity::fromModel))
        dao.prune(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
    }
}