package com.zenplayer.app.data.source

import com.zenplayer.app.data.model.Channel
import com.zenplayer.app.data.model.MediaType
import com.zenplayer.app.data.net.HttpEngine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.MessageDigest

class StalkerClient(
    private val portal: String,
    private val mac: String,
    private val providerId: Long = 0L
) {

    private val json = Json { ignoreUnknownKeys = true }
    private val baseUrl = portal.trimEnd('/')
    private val normalizedMac = normalizeMac(mac)

    data class Session(val token: String, val serverId: String, val login: String, val expiry: String?)

    private companion object {
        const val USER_AGENT = "Mozilla/5.0 (QtEmbedded; U; Linux; C) AppleWebKit/533.3 (KHTML, like Gecko) MAG250 stbapp ver: 2 rev: 250 Safari/533.3"
    }

    suspend fun authenticate(): Session {
        val token = fetchToken()
        val serverId = handshake(token)
        val login = doAuth(token, serverId)
        val expiry = try { getProfile(token, serverId) } catch (_: Exception) { null }
        return Session(token, serverId, login, expiry)
    }

    suspend fun loadChannels(session: Session): List<Channel> {
        val url = portalUrl(
            action = "get_allowed_stb",
            token = session.token,
            serverId = session.serverId,
            extra = "&type=all"
        )
        val body = baseBody(session.login) + "&type=all"
        val response = try {
            HttpEngine.post(url, body, stalkHeaders())
        } catch (_: Exception) {
            return emptyList()
        }

        return extractChannelArray(response).mapIndexedNotNull { index, obj ->
            val id = obj["id"]?.jsonPrimitive?.content
                ?: obj["channel_id"]?.jsonPrimitive?.content
            if (id == null) return@mapIndexedNotNull null

            val mediaType = classify(obj["tv_genre_id"]?.jsonPrimitive?.content, obj)
            val name = obj["name"]?.jsonPrimitive?.content ?: "Kanal"
            val logo = obj["logo"]?.jsonPrimitive?.content?.ifBlank { null }
                ?: obj["icon"]?.jsonPrimitive?.content?.ifBlank { null }
            val category = obj["tv_genre_name"]?.jsonPrimitive?.content?.ifBlank { null }
                ?: obj["category_name"]?.jsonPrimitive?.content?.ifBlank { null }

            // Prefer cmd returned by the channel list; otherwise resolve via create_link.
            val cmd = obj["cmd"]?.jsonPrimitive?.content?.ifBlank { null }
            val streamUrl = cmd?.let { normalizeCmd(it) } ?: resolveCreateLink(session, id, mediaType)

            Channel(
                id = "stalker_${providerId}_$id",
                providerId = providerId,
                mediaType = mediaType,
                name = name,
                url = streamUrl,
                logoUrl = logo,
                category = category,
                number = index + 1,
                extra = cmd
            )
        }
    }

    suspend fun epgUrl(): String? = try {
        "$baseUrl/xmltv.php?type=itv&token=${fetchToken()}"
    } catch (_: Exception) {
        null
    }

    private fun stalkHeaders(): Map<String, String> = mapOf(
        "User-Agent" to USER_AGENT,
        "Cookie" to "mac=$normalizedMac; stb_lang=en; timezone=Europe%2FBerlin",
        "X-Forwarded-For" to randomIp(),
        "Accept-Language" to "en-US,*",
        "Referer" to "$baseUrl/"
    )

    private fun normalizeMac(input: String): String {
        val digits = input.filter { it.isLetterOrDigit() }.uppercase()
        return when (digits.length) {
            12 -> digits.chunked(2).joinToString(":")
            else -> input.trim().uppercase()
        }
    }

    private fun randomIp(): String =
        "${(1..254).random()}.${(0..255).random()}.${(0..255).random()}.${(1..254).random()}"

    private fun classify(genreId: String?, obj: JsonObject): MediaType {
        val genre = genreId?.lowercase() ?: ""
        val text = obj.toString().lowercase()
        return when {
            genre in listOf("radio", "music") -> MediaType.MUSIC
            genre in listOf("series") || text.contains("\"series\"") -> MediaType.SERIES
            genre in listOf("vod", "film", "movie") || text.contains("movie") || text.contains("vod") -> MediaType.VOD
            else -> MediaType.LIVE
        }
    }

    private suspend fun fetchToken(): String {
        val url = "$baseUrl/portal.php?type=stb&action=get_token"
        val raw = HttpEngine.get(url, stalkHeaders())
        val root = json.parseToJsonElement(raw).jsonObject
        val token = root["token"]?.jsonPrimitive?.content
            ?: root["js"]?.jsonObject?.get("token")?.jsonPrimitive?.content
        return token ?: error("Kein Token vom Stalker-Portal erhalten")
    }

    private suspend fun handshake(token: String): String {
        val url = portalUrl("handshake", token, "", "")
        val body = "type=stb&action=handshake&token=$token&JsHttpRequest=1-xml"
        val response = HttpEngine.post(url, body, stalkHeaders())
        val js = json.parseToJsonElement(response).jsonObject["js"]?.jsonObject
        val serverId = js?.get("server_id")?.jsonPrimitive?.content
        if (!serverId.isNullOrBlank()) return serverId
        val rawJs = js?.toString() ?: response
        val regex = Regex("""server_id\s*[:=]\s*["']?(\d+)["']?""")
        return regex.find(rawJs)?.groupValues?.get(1) ?: "1"
    }

    private suspend fun doAuth(token: String, serverId: String): String {
        val url = portalUrl("do_auth", token, serverId, "")
        val body = baseBody(normalizedMac) +
            "&token=$token&server_id=$serverId&hd=1&stb_type=MAG250&client_type=STB&image_version=&time_zone=Europe%2FBerlin" +
            "&created=${System.currentTimeMillis() / 1000}&serial_number=0&signature=${md5("${normalizedMac}$token")}"
        val response = HttpEngine.post(url, body, stalkHeaders())
        val obj = json.parseToJsonElement(response).jsonObject
        return obj["login"]?.jsonPrimitive?.content
            ?: obj["js"]?.jsonObject?.get("login")?.jsonPrimitive?.content
            ?: normalizedMac
    }

    private suspend fun getProfile(token: String, serverId: String): String? {
        val url = portalUrl("get_profile", token, serverId, "")
        val body = baseBody(normalizedMac) + "&token=$token&server_id=$serverId&was_active="
        val response = HttpEngine.post(url, body, stalkHeaders())
        val jsObj = json.parseToJsonElement(response).jsonObject["js"]?.jsonObject
        jsObj?.get("expire_date")?.jsonPrimitive?.content?.let { return it }
        val js = jsObj?.toString() ?: ""
        return Regex("""expire_date\s*[:=]\s*["']?([\d\-.]+)["']?""")
            .find(js)?.groupValues?.get(1)
    }

    private suspend fun resolveCreateLink(session: Session, channelId: String, mediaType: MediaType): String {
        val typeParam = if (mediaType == MediaType.VOD || mediaType == MediaType.SERIES) "vod" else "itv"
        val url = "$baseUrl/portal.php?type=$typeParam&action=create_link&cmd=$channelId&series=&forced_storage=undefined&disable_ad=0&download=0&force_ch_link_check=0"
        return try {
            val response = HttpEngine.post(url, "", stalkHeaders())
            val js = json.parseToJsonElement(response).jsonObject["js"]?.jsonObject
            val cmd = js?.get("cmd")?.jsonPrimitive?.content
            val link = js?.get("link")?.jsonPrimitive?.content
            cmd?.let { normalizeCmd(it) }
                ?: link?.ifBlank { null }
                ?: url
        } catch (_: Exception) {
            url
        }
    }

    private fun normalizeCmd(cmd: String): String {
        val trimmed = cmd.trim()
        return if (trimmed.lowercase().startsWith("ffmpeg ")) {
            trimmed.substring(7).trim()
        } else {
            trimmed
        }
    }

    private fun portalUrl(action: String, token: String, serverId: String, extra: String): String {
        val base = "$baseUrl/portal.php?type=stb"
        return "$base&action=$action&token=$token${if (serverId.isNotBlank()) "&server_id=$serverId" else ""}$extra"
    }

    private fun baseBody(login: String): String =
        "type=stb&login=${login.encode()}&password="

    private fun extractChannelArray(response: String): List<JsonObject> {
        val root = try {
            json.parseToJsonElement(response).jsonObject
        } catch (_: Exception) {
            return emptyList()
        }
        val js = root["js"]?.jsonObject
        val data = js?.get("data")?.jsonArray
        if (data != null) {
            return data.mapNotNull { it as? JsonObject }
        }
        val jsArray = try {
            root["js"]?.jsonArray
        } catch (_: Exception) { null }
        if (jsArray != null) {
            return jsArray.mapNotNull { it as? JsonObject }
        }
        // Last resort: grab the first JSON array in the response.
        val jsonPart = Regex("""\[.*\]""", RegexOption.DOT_MATCHES_ALL)
            .find(response)?.value ?: return emptyList()
        return runCatching {
            json.parseToJsonElement(jsonPart).jsonArray.mapNotNull { it as? JsonObject }
        }.getOrDefault(emptyList())
    }

    private fun md5(input: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun String.encode(): String = java.net.URLEncoder.encode(this, "UTF-8")
}
