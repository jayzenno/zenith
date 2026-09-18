package com.zenplayer.app.data.repo

import com.zenplayer.app.data.db.ChannelDao
import com.zenplayer.app.data.model.Channel
import com.zenplayer.app.data.model.MediaType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChannelRepository(private val dao: ChannelDao) {

    fun channelsByProvider(providerId: Long): Flow<List<Channel>> =
        dao.observeByProvider(providerId).map { list -> list.map { it.toModel() } }

    fun channelsOfType(providerId: Long, type: MediaType): Flow<List<Channel>> =
        dao.observeByType(providerId, type.name).map { list -> list.map { it.toModel() } }

    fun channelsOfCategory(providerId: Long, type: MediaType, category: String): Flow<List<Channel>> =
        dao.observeByCategory(providerId, type.name, category).map { list -> list.map { it.toModel() } }

    fun categories(providerId: Long, type: MediaType): Flow<List<String>> =
        dao.observeCategories(providerId, type.name)

    fun allLiveChannels(): Flow<List<Channel>> =
        dao.observeLive().map { list -> list.map { it.toModel() } }

    suspend fun getChannel(id: String): Channel? = dao.getById(id)?.toModel()

    suspend fun countByType(providerId: Long, type: MediaType): Int =
        dao.countByType(providerId, type.name)
}