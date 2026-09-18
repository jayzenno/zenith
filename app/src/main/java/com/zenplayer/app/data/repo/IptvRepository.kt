package com.zenplayer.app.data.repo

import android.content.Context
import com.zenplayer.app.data.db.ChannelEntity
import com.zenplayer.app.data.db.ProviderDao
import com.zenplayer.app.data.db.ProviderEntity
import com.zenplayer.app.data.db.ZenDatabase
import com.zenplayer.app.data.model.Channel
import com.zenplayer.app.data.model.MediaType
import com.zenplayer.app.data.model.Provider
import com.zenplayer.app.data.model.ProviderType
import com.zenplayer.app.data.net.HttpEngine
import com.zenplayer.app.data.parser.EpgParser
import com.zenplayer.app.data.parser.M3uParser
import com.zenplayer.app.data.source.StalkerClient
import com.zenplayer.app.data.source.XtreamClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class SyncResult(
    val ok: Boolean,
    val message: String,
    val channelCount: Int = 0
)

class IptvRepository(
    context: Context,
    private val providerDao: ProviderDao,
    private val channelDao: com.zenplayer.app.data.db.ChannelDao,
    private val epgDao: com.zenplayer.app.data.db.EpgDao,
    private val db: ZenDatabase
) {

    val providersFlow: Flow<List<Provider>> =
        providerDao.observeAll().map { list -> list.map { it.toModel() } }

    suspend fun getProvider(id: Long): Provider? = providerDao.getById(id)?.toModel()

    suspend fun providers(): List<Provider> = providerDao.getAll().map { it.toModel() }

    suspend fun addProvider(provider: Provider): Long {
        val copy = provider.copy(id = 0L)
        return withContext(Dispatchers.IO) {
            providerDao.upsert(ProviderEntity.fromModel(copy))
        }
    }

    suspend fun updateProvider(provider: Provider) = withContext(Dispatchers.IO) {
        providerDao.update(
            id = provider.id,
            name = provider.name,
            type = provider.type.name,
            url = provider.url,
            username = provider.username,
            password = provider.password,
            mac = provider.mac,
            epgUrl = provider.epgUrl,
            lastUpdated = provider.lastUpdated
        )
    }

    suspend fun deleteProvider(provider: Provider) = withContext(Dispatchers.IO) {
        channelDao.deleteByProvider(provider.id)
        epgDao.deleteByProvider(provider.id)
        providerDao.delete(ProviderEntity.fromModel(provider))
    }

    suspend fun syncProvider(provider: Provider): SyncResult = withContext(Dispatchers.IO) {
        val result = try {
            when (provider.type) {
                ProviderType.M3U -> syncM3U(provider)
                ProviderType.XTREAM -> syncXtream(provider)
                ProviderType.STALKER -> syncStalker(provider)
            }
        } catch (e: Exception) {
            SyncResult(ok = false, message = e.message ?: "Unbekannter Fehler")
        }
        if (result.ok) {
            providerDao.update(
                id = provider.id,
                name = provider.name,
                type = provider.type.name,
                url = provider.url,
                username = provider.username,
                password = provider.password,
                mac = provider.mac,
                epgUrl = provider.epgUrl,
                lastUpdated = System.currentTimeMillis()
            )
        }
        result
    }

    suspend fun syncEpg(provider: Provider): SyncResult = withContext(Dispatchers.IO) {
        val url = when (provider.type) {
            ProviderType.XTREAM -> try {
                requireXtream(provider).epgUrl()
            } catch (e: Exception) {
                return@withContext SyncResult(false, e.message ?: "Xtream-Daten fehlen")
            }
            else -> provider.epgUrl
        }
        if (url.isNullOrBlank()) return@withContext SyncResult(false, "Keine EPG-URL verfügbar")

        val buffer = ArrayList<com.zenplayer.app.data.db.EpgProgramEntity>(2048)
        var total = 0
        try {
            HttpEngine.openStream(url) { input ->
                EpgParser.parseStream(input.reader(), provider.id) { program ->
                    buffer.add(com.zenplayer.app.data.db.EpgProgramEntity.fromModel(program))
                    total++
                    if (buffer.size >= 2000) {
                        epgDao.upsertAll(buffer.toList())
                        buffer.clear()
                    }
                }
            }
        } catch (e: Exception) {
            if (buffer.isNotEmpty()) epgDao.upsertAll(buffer.toList())
            return@withContext SyncResult(false, e.message ?: "EPG-Synchronisation fehlgeschlagen", total)
        }
        if (buffer.isNotEmpty()) epgDao.upsertAll(buffer.toList())
        if (total == 0) return@withContext SyncResult(false, "Keine EPG-Programme gefunden")
        epgDao.prune(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
        SyncResult(true, "EPG aktualisiert", total)
    }

    private suspend fun syncM3U(provider: Provider): SyncResult {
        if (provider.url.isBlank()) return SyncResult(false, "Keine M3U-URL angegeben")
        channelDao.deleteByProvider(provider.id)

        val counters = mutableMapOf<String, Int>()
        val buffer = ArrayList<Channel>(2048)
        var total = 0

        val parser = M3uParser.StreamParser(provider.url) { entry ->
            val type = M3uParser.classifyMediaType(entry)
            val counter = (counters[type.name] ?: 0) + 1
            counters[type.name] = counter
            buffer.add(
                Channel(
                    id = M3uParser.channelId(provider.id, entry),
                    providerId = provider.id,
                    mediaType = type,
                    name = entry.name,
                    url = entry.url,
                    logoUrl = entry.logo,
                    category = entry.group,
                    number = counter,
                    extra = entry.tvgId
                )
            )
            total++
        }

        try {
            HttpEngine.streamLines(provider.url) { line ->
                parser.accept(line)
                if (buffer.size >= 2000) {
                    channelDao.upsertAll(buffer.map(ChannelEntity::fromModel))
                    buffer.clear()
                }
            }
        } catch (e: Exception) {
            if (buffer.isNotEmpty()) channelDao.upsertAll(buffer.map(ChannelEntity::fromModel))
            return SyncResult(false, "Playlist konnte nicht geladen werden: ${e.message}", total)
        }

        if (buffer.isNotEmpty()) channelDao.upsertAll(buffer.map(ChannelEntity::fromModel))
        if (total == 0) return SyncResult(false, "Keine Kanäle in Playlist gefunden")
        return SyncResult(true, "Playlist geladen", total)
    }

    private suspend fun syncXtream(provider: Provider): SyncResult {
        val client = requireXtream(provider)
        val ok = client.login()
        if (!ok) return SyncResult(false, "Xtream-Login fehlgeschlagen")

        val all = mutableListOf<Channel>()
        all += client.loadChannels("live", client.loadCategories("live"))
        all += client.loadChannels("vod", client.loadCategories("vod"))
        all += client.loadChannels("series", client.loadCategories("series"))
        if (all.isEmpty()) return SyncResult(false, "Keine Streams vom Server empfangen")
        persistChannels(provider.id, all)
        return SyncResult(true, "Xtream synchronisiert", all.size)
    }

    private suspend fun syncStalker(provider: Provider): SyncResult {
        val mac = provider.mac?.takeIf { it.isNotBlank() }
            ?: return SyncResult(false, "MAC-Adresse für Stalker-Portal fehlt")
        val client = StalkerClient(provider.url, mac, provider.id)
        val session = try {
            client.authenticate()
        } catch (e: Exception) {
            return SyncResult(false, "Stalker-Login fehlgeschlagen: ${e.message}")
        }
        val channels = try {
            client.loadChannels(session)
        } catch (e: Exception) {
            return SyncResult(false, "Stalker-Kanalliste fehlgeschlagen: ${e.message}")
        }
        if (channels.isEmpty()) return SyncResult(false, "Keine Kanäle vom Portal erhalten")
        persistChannels(provider.id, channels)
        return SyncResult(true, "Stalker-Portal synchronisiert", channels.size)
    }

    private suspend fun persistChannels(providerId: Long, channels: List<Channel>) {
        channelDao.deleteByProvider(providerId)
        channelDao.upsertAll(channels.map(ChannelEntity::fromModel))
    }

    private fun requireXtream(provider: Provider): XtreamClient {
        val user = provider.username?.takeIf { it.isNotBlank() }
            ?: error("Benutzername fehlt")
        val pass = provider.password?.takeIf { it.isNotBlank() }
            ?: error("Passwort fehlt")
        return XtreamClient(provider.url.trimEnd('/'), user, pass, provider.id)
    }
}
