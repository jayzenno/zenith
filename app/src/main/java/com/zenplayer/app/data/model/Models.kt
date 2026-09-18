package com.zenplayer.app.data.model

enum class ProviderType {
    M3U, XTREAM, STALKER
}

enum class MediaType {
    LIVE, VOD, MUSIC, SERIES
}

data class Provider(
    val id: Long = 0L,
    val name: String,
    val type: ProviderType,
    val url: String,
    val username: String? = null,
    val password: String? = null,
    val mac: String? = null,
    val epgUrl: String? = null,
    val lastUpdated: Long = 0L
)

data class Channel(
    val id: String,
    val providerId: Long,
    val mediaType: MediaType,
    val name: String,
    val url: String,
    val logoUrl: String? = null,
    val category: String? = null,
    val number: Int = 0,
    val extra: String? = null
)

data class EpgProgram(
    val id: String,
    val channelId: String,
    val providerId: Long,
    val startTs: Long,
    val endTs: Long,
    val title: String,
    val description: String? = null
)