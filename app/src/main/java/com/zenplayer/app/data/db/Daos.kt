package com.zenplayer.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProviderDao {

    @Query("SELECT * FROM providers ORDER BY name")
    fun observeAll(): Flow<List<ProviderEntity>>

    @Query("SELECT * FROM providers ORDER BY name")
    suspend fun getAll(): List<ProviderEntity>

    @Query("SELECT * FROM providers WHERE id = :id")
    suspend fun getById(id: Long): ProviderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(provider: ProviderEntity): Long

    @Query("UPDATE providers SET name=:name, type=:type, url=:url, username=:username, password=:password, mac=:mac, epgUrl=:epgUrl, lastUpdated=:lastUpdated WHERE id=:id")
    suspend fun update(
        id: Long,
        name: String,
        type: String,
        url: String,
        username: String?,
        password: String?,
        mac: String?,
        epgUrl: String?,
        lastUpdated: Long
    )

    @Delete
    suspend fun delete(provider: ProviderEntity)
}

@Dao
interface ChannelDao {

    @Query("SELECT * FROM channels WHERE providerId = :providerId ORDER BY number, name")
    fun observeByProvider(providerId: Long): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE providerId = :providerId AND mediaType = :mediaType ORDER BY number, name")
    fun observeByType(providerId: Long, mediaType: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE providerId = :providerId AND mediaType = :mediaType AND category = :category ORDER BY number, name")
    fun observeByCategory(providerId: Long, mediaType: String, category: String): Flow<List<ChannelEntity>>

    @Query("SELECT DISTINCT category FROM channels WHERE providerId = :providerId AND mediaType = :mediaType AND category IS NOT NULL ORDER BY category")
    fun observeCategories(providerId: Long, mediaType: String): Flow<List<String>>

    @Query("SELECT * FROM channels WHERE id = :id")
    suspend fun getById(id: String): ChannelEntity?

    @Query("SELECT * FROM channels WHERE mediaType = 'LIVE' ORDER BY number, name")
    fun observeLive(): Flow<List<ChannelEntity>>

    @Query("SELECT COUNT(*) FROM channels WHERE providerId = :providerId AND mediaType = :mediaType")
    suspend fun countByType(providerId: Long, mediaType: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(channels: List<ChannelEntity>)

    @Query("DELETE FROM channels WHERE providerId = :providerId")
    suspend fun deleteByProvider(providerId: Long)
}

@Dao
interface EpgDao {

    @Query("SELECT * FROM epg_programs WHERE channelId = :channelId AND endTs >= :now ORDER BY startTs ASC LIMIT 8")
    fun observeUpcoming(channelId: String, now: Long): Flow<List<EpgProgramEntity>>

    @Query("SELECT * FROM epg_programs WHERE channelId = :channelId ORDER BY startTs ASC")
    suspend fun getForChannel(channelId: String): List<EpgProgramEntity>

    @Query("SELECT * FROM epg_programs WHERE channelId = :channelId AND startTs < :dayEnd AND endTs > :dayStart ORDER BY startTs ASC")
    suspend fun getForChannelAndWindow(channelId: String, dayStart: Long, dayEnd: Long): List<EpgProgramEntity>

    @Query("SELECT * FROM epg_programs WHERE startTs < :dayEnd AND endTs > :dayStart ORDER BY channelId ASC, startTs ASC")
    suspend fun getForWindow(dayStart: Long, dayEnd: Long): List<EpgProgramEntity>

    @Query("DELETE FROM epg_programs")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(programs: List<EpgProgramEntity>)

    @Query("DELETE FROM epg_programs WHERE endTs < :before")
    suspend fun prune(before: Long)

    @Query("DELETE FROM epg_programs WHERE providerId = :providerId")
    suspend fun deleteByProvider(providerId: Long)
}