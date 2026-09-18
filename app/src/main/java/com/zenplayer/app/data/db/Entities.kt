package com.zenplayer.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zenplayer.app.data.model.Channel
import com.zenplayer.app.data.model.EpgProgram
import com.zenplayer.app.data.model.MediaType
import com.zenplayer.app.data.model.Provider
import com.zenplayer.app.data.model.ProviderType

@Entity(tableName = "providers")
data class ProviderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val type: String,
    val url: String,
    val username: String? = null,
    val password: String? = null,
    val mac: String? = null,
    val epgUrl: String? = null,
    val lastUpdated: Long = 0L
) {
    fun toModel() = Provider(
        id = id,
        name = name,
        type = ProviderType.valueOf(type),
        url = url,
        username = username,
        password = password,
        mac = mac,
        epgUrl = epgUrl,
        lastUpdated = lastUpdated
    )

    companion object {
        fun fromModel(p: Provider) = ProviderEntity(
            id = p.id,
            name = p.name,
            type = p.type.name,
            url = p.url,
            username = p.username,
            password = p.password,
            mac = p.mac,
            epgUrl = p.epgUrl,
            lastUpdated = p.lastUpdated
        )
    }
}

@Entity(
    tableName = "channels",
    indices = [Index("providerId"), Index("mediaType"), Index("category")]
)
data class ChannelEntity(
    @PrimaryKey val id: String,
    val providerId: Long,
    val mediaType: String,
    val name: String,
    val url: String,
    val logoUrl: String? = null,
    val category: String? = null,
    val number: Int = 0,
    val extra: String? = null
) {
    fun toModel() = Channel(
        id = id,
        providerId = providerId,
        mediaType = MediaType.valueOf(mediaType),
        name = name,
        url = url,
        logoUrl = logoUrl,
        category = category,
        number = number,
        extra = extra
    )

    companion object {
        fun fromModel(c: Channel) = ChannelEntity(
            id = c.id,
            providerId = c.providerId,
            mediaType = c.mediaType.name,
            name = c.name,
            url = c.url,
            logoUrl = c.logoUrl,
            category = c.category,
            number = c.number,
            extra = c.extra
        )
    }
}

@Entity(
    tableName = "epg_programs",
    indices = [Index("channelId"), Index("startTs")]
)
data class EpgProgramEntity(
    @PrimaryKey val id: String,
    val channelId: String,
    val providerId: Long,
    val startTs: Long,
    val endTs: Long,
    val title: String,
    val description: String? = null
) {
    fun toModel() = EpgProgram(
        id = id,
        channelId = channelId,
        providerId = providerId,
        startTs = startTs,
        endTs = endTs,
        title = title,
        description = description
    )

    companion object {
        fun fromModel(p: EpgProgram) = EpgProgramEntity(
            id = p.id,
            channelId = p.channelId,
            providerId = p.providerId,
            startTs = p.startTs,
            endTs = p.endTs,
            title = p.title,
            description = p.description
        )
    }
}