package com.zenplayer.app.data.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit

object HttpEngine {

    private const val DEFAULT_USER_AGENT = "ZenPlayer/0.1 (Android TV)"

    @Volatile
    var userAgent: String = DEFAULT_USER_AGENT

    @Volatile
    var defaultConnectTimeoutSeconds: Int = 20

    @Volatile
    var defaultReadTimeoutSeconds: Int = 90

    @Volatile
    private var client: OkHttpClient = newClient()

    private fun newClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(defaultConnectTimeoutSeconds.toLong(), TimeUnit.SECONDS)
            .readTimeout(defaultReadTimeoutSeconds.toLong(), TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

    @Synchronized
    fun applyTimeouts(connectSeconds: Int, readSeconds: Int) {
        defaultConnectTimeoutSeconds = connectSeconds
        defaultReadTimeoutSeconds = readSeconds
        client = newClient()
    }

    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .apply { headers.forEach { (k, v) -> header(k, v) } }
            .header("User-Agent", headers["User-Agent"] ?: userAgent)
            .build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code} für $url")
            resp.body?.string() ?: error("Leerer Body für $url")
        }
    }

    suspend fun getBytes(url: String, headers: Map<String, String> = emptyMap()): ByteArray = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .apply { headers.forEach { (k, v) -> header(k, v) } }
            .header("User-Agent", headers["User-Agent"] ?: userAgent)
            .build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code} für $url")
            resp.body?.bytes() ?: error("Leerer Body für $url")
        }
    }

    suspend fun streamLines(
        url: String,
        headers: Map<String, String> = emptyMap(),
        onLine: suspend (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .apply { headers.forEach { (k, v) -> header(k, v) } }
            .header("User-Agent", headers["User-Agent"] ?: userAgent)
            .build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code} für $url")
            val source = resp.body?.source() ?: error("Leerer Body für $url")
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                onLine(line)
            }
        }
    }

    suspend fun <T> openStream(
        url: String,
        headers: Map<String, String> = emptyMap(),
        block: suspend (java.io.InputStream) -> T
    ): T = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .apply { headers.forEach { (k, v) -> header(k, v) } }
            .header("User-Agent", headers["User-Agent"] ?: userAgent)
            .build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code} für $url")
            val body = resp.body ?: error("Leerer Body für $url")
            block(body.byteStream())
        }
    }

    suspend fun post(url: String, body: String, headers: Map<String, String> = emptyMap()): String = withContext(Dispatchers.IO) {
        val reqBody: RequestBody = body.toRequestBody("application/x-www-form-urlencoded".toMediaType())
        val request = Request.Builder()
            .url(url)
            .apply { headers.forEach { (k, v) -> header(k, v) } }
            .header("User-Agent", headers["User-Agent"] ?: userAgent)
            .post(reqBody)
            .build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code} für $url")
            resp.body?.string() ?: error("Leerer Body für $url")
        }
    }

    fun resolve(base: String?, ref: String): String {
        if (ref.startsWith("http://") || ref.startsWith("https://")) return ref
        if (base.isNullOrBlank()) return ref
        val b = base.trimEnd('/')
        return if (ref.startsWith("/")) {
            val scheme = b.substringBefore("//")
            val host = b.substringAfter("//").substringBefore("/")
            "$scheme//$host$ref"
        } else {
            "$b/$ref"
        }
    }
}
