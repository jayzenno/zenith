package com.zenplayer.app.data.source

import com.zenplayer.app.data.model.Channel
import com.zenplayer.app.data.model.MediaType
import com.zenplayer.app.data.net.HttpEngine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class XtreamClient(
    private val base: String,
    private val user: String,
    private val pass: String,
    private val providerId: Long = 0L
) {

    private val json = Json { ignoreUnknownKeys = true }
    private val normalizedBase = base.trimEnd('/')

    private data class ServerInfo(
        val host: String,
        val port: String,
        val output: String,
        val httpsPort: String? = null,
        val serverProtocol: String? = null,
        val rtmpPort: String? = null
    )

    private object Keys {
        const val API = "player_api.php"
        const val XMLTV = "xmltv.php"
        const val GET = "get.php"
    }

    suspend fun login(): Boolean {
        val url = "$normalizedBase/${Keys.API}?username=${user.encode()}&password=${pass.encode()}"
        return try {
            val root = json.parseToJsonElement(HttpEngine.get(url)).jsonObject
            val userInfo = root["user_info"]?.jsonObject
            val status = userInfo?.get("status")?.jsonPrimitive?.content?.lowercase().orEmpty()
            // Many servers answer with status "Active", "active", "1", "OK" etc.
            if (status.isNotBlank() && status != "disabled" && status != "banned" && status != "0") {
                return true
            }
            // Some servers omit status but include the authenticated username.
            val returnedUser = userInfo?.get("username")?.jsonPrimitive?.content
            if (returnedUser?.equals(user, ignoreCase = true) == true) return true
            // As a last resort accept any non-empty user_info block.
            userInfo?.isNotEmpty() == true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun loadCategories(type: String): Map<String, String> {
        val action = when (type) {
            "live" -> "get_live_categories"
            "vod" -> "get_vod_categories"
            else -> "get_series_categories"
        }
        val url = "$normalizedBase/${Keys.API}?username=${user.encode()}&password=${pass.encode()}&action=$action"
        return try {
            val arr = json.parseToJsonElement(HttpEngine.get(url)) as? JsonArray ?: return emptyMap()
            arr.mapNotNull { elem ->
                val obj = elem as? JsonObject ?: return@mapNotNull null
                val id = obj["category_id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val name = obj["category_name"]?.jsonPrimitive?.content ?: ""
                id to name
            }.toMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    suspend fun loadChannels(type: String, categoryNames: Map<String, String>): List<Channel> {
        val action = when (type) {
            "live" -> "get_live_streams"
            "vod" -> "get_vod_streams"
            else -> "get_series"
        }
        val url = "$normalizedBase/${Keys.API}?username=${user.encode()}&password=${pass.encode()}&action=$action"
        val raw = try {
            HttpEngine.get(url)
        } catch (_: Exception) {
            return emptyList()
        }

        val arr = try {
            json.parseToJsonElement(raw) as? JsonArray ?: return emptyList()
        } catch (_: Exception) {
            return emptyList()
        }

        val server = loadServerInfo()
        return arr.mapIndexedNotNull { index, elem ->
            val obj = elem as? JsonObject ?: return@mapIndexedNotNull null
            val streamId = when (type) {
                "live", "vod" -> obj["stream_id"]?.jsonPrimitive?.content
                else -> obj["series_id"]?.jsonPrimitive?.content
            } ?: return@mapIndexedNotNull null

            val name = obj["name"]?.jsonPrimitive?.content ?: "Unbenannt"
            val logo = obj["stream_icon"]?.jsonPrimitive?.content
                ?: obj["cover"]?.jsonPrimitive?.content
                ?: obj["thumbnail"]?.jsonPrimitive?.content
            val categoryId = obj["category_id"]?.jsonPrimitive?.content
            val categoryName = categoryNames[categoryId]
            val num = obj["num"]?.jsonPrimitive?.content?.toIntOrNull() ?: (index + 1)

            val mediaType = when (type) {
                "live" -> MediaType.LIVE
                "vod" -> MediaType.VOD
                else -> MediaType.SERIES
            }

            val container = when (mediaType) {
                MediaType.VOD -> obj["container_extension"]?.jsonPrimitive?.content?.ifBlank { null }
                    ?: "mkv"
                MediaType.LIVE -> server.output
                else -> "mkv"
            }

            val streamUrl = buildStreamUrl(type, server, streamId, container)
            val extra = obj["epg_channel_id"]?.jsonPrimitive?.content

            Channel(
                id = "xtream_${providerId}_${type}_$streamId",
                providerId = providerId,
                mediaType = mediaType,
                name = name,
                url = streamUrl,
                logoUrl = logo,
                category = categoryName,
                number = num,
                extra = if (mediaType == MediaType.SERIES) "series:$streamId" else extra
            )
        }
    }

    suspend fun epgUrl(): String? = try {
        "$normalizedBase/${Keys.XMLTV}?username=${user.encode()}&password=${pass.encode()}"
    } catch (_: Exception) {
        null
    }

    suspend fun m3uUrl(): String =
        "$normalizedBase/${Keys.GET}?username=${user.encode()}&password=${pass.encode()}&type=m3u_plus&output=${desiredOutput()}"

    private suspend fun loadServerInfo(): ServerInfo {
        val url = "$normalizedBase/${Keys.API}?username=${user.encode()}&password=${pass.encode()}"
        return try {
            val obj = json.parseToJsonElement(HttpEngine.get(url)).jsonObject
            val server = obj["server_info"]?.jsonObject
            parseServerInfo(server)
        } catch (_: Exception) {
            fallbackServerInfo()
        }
    }

    private fun parseServerInfo(server: JsonObject?): ServerInfo {
        val url = server?.get("url")?.jsonPrimitive?.content?.ifBlank { null }
        val port = server?.get("port")?.jsonPrimitive?.content?.ifBlank { null }
        val httpsPort = server?.get("https_port")?.jsonPrimitive?.content?.ifBlank { null }
        val protocol = server?.get("server_protocol")?.jsonPrimitive?.content?.ifBlank { null }
        val rtmpPort = server?.get("rtmp_port")?.jsonPrimitive?.content?.ifBlank { null }
        val output = server?.get("stream_output_format")?.jsonPrimitive?.content
        val parsed = parseBaseUrl(normalizedBase)
        val host = extractHost(url) ?: parsed.host
        val finalPort = port ?: httpsPort ?: parsed.port
        val finalProtocol = protocol ?: if (normalizedBase.startsWith("https")) "https" else "http"
        return ServerInfo(
            host = host,
            port = finalPort,
            output = resolveOutput(output),
            httpsPort = httpsPort,
            serverProtocol = finalProtocol,
            rtmpPort = rtmpPort
        )
    }

    private fun fallbackServerInfo(): ServerInfo {
        val parsed = parseBaseUrl(normalizedBase)
        return ServerInfo(
            host = parsed.host,
            port = parsed.port,
            output = if (normalizedBase.startsWith("https")) "m3u8" else "ts"
        )
    }

    private data class ParsedBase(val host: String, val port: String)

    private fun parseBaseUrl(url: String): ParsedBase {
        val withoutScheme = url.substringAfter("://", url)
        val hostPort = withoutScheme.substringBefore("/")
        val host = hostPort.substringBefore(":")
        val port = hostPort.substringAfter(":", "").ifBlank {
            if (url.startsWith("https")) "443" else "80"
        }
        return ParsedBase(host, port)
    }

    private fun extractHost(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val withoutScheme = url.substringAfter("://", url)
        return withoutScheme.substringBefore(":").substringBefore("/")
    }

    private fun resolveOutput(output: String?): String {
        if (output.isNullOrBlank()) return "ts"
        return when {
            output.contains("hls", ignoreCase = true) -> "m3u8"
            output.contains("mpegts", ignoreCase = true) -> "ts"
            else -> output
        }
    }

    private fun buildStreamUrl(type: String, server: ServerInfo, id: String, container: String): String {
        val path = when (type) {
            "live" -> "live"
            "vod" -> "movie"
            "series" -> "series"
            else -> "live"
        }
        val protocol = server.serverProtocol ?: if (normalizedBase.startsWith("https")) "https" else "http"
        val port = when {
            protocol == "https" && !server.httpsPort.isNullOrBlank() -> server.httpsPort
            else -> server.port
        }
        val portSuffix = if ((protocol == "https" && port == "443") || (protocol == "http" && port == "80")) "" else ":$port"
        return "$protocol://${server.host}$portSuffix/$path/${user.encode()}/${pass.encode()}/$id.$container"
    }

    private fun desiredOutput(): String = "ts"

    private fun String.encode(): String {
        return java.net.URLEncoder.encode(this, "UTF-8")
    }
}
