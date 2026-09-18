package com.zenplayer.app.data.parser

import com.zenplayer.app.data.model.EpgProgram
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.Reader
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object EpgParser {

    private val DATE_FORMATS = listOf(
        SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US),
        SimpleDateFormat("yyyyMMddHHmmss", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    )

    suspend fun parse(xml: String, providerId: Long): List<EpgProgram> {
        val programs = ArrayList<EpgProgram>()
        parseStream(StringReader(xml), providerId) { programs.add(it) }
        return programs
    }

    /**
     * Streams the XMLTV document and emits each programme as soon as it is complete,
     * so very large EPG files do not have to be held in memory.
     */
    suspend fun parseStream(reader: Reader, providerId: Long, onProgram: suspend (EpgProgram) -> Unit) {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(reader)

        var eventType = parser.eventType
        var currentChannelId: String? = null
        var currentStart = 0L
        var currentEnd = 0L
        var currentTitle: String? = null
        var currentDesc: String? = null
        var insideTag: String? = null
        val textBuffer = StringBuilder()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "programme" -> {
                        currentChannelId = parser.getAttributeValue(null, "channel") ?: ""
                        currentStart = parseTime(parser.getAttributeValue(null, "start"))
                        currentEnd = parseTime(parser.getAttributeValue(null, "stop"))
                        currentTitle = null
                        currentDesc = null
                        insideTag = null
                    }

                    "title", "desc" -> {
                        insideTag = parser.name
                        textBuffer.clear()
                    }
                }

                XmlPullParser.TEXT -> insideTag?.let { textBuffer.append(parser.text) }

                XmlPullParser.END_TAG -> when (parser.name) {
                    "title" -> {
                        currentTitle = textBuffer.toString().trim().ifBlank { null }
                        insideTag = null
                    }

                    "desc" -> {
                        currentDesc = textBuffer.toString().trim().ifBlank { null }
                        insideTag = null
                    }

                    "programme" -> {
                        val chId = currentChannelId
                        if (chId != null && currentStart > 0 && !currentTitle.isNullOrBlank()) {
                            onProgram(
                                EpgProgram(
                                    id = "$providerId:$chId:$currentStart",
                                    channelId = chId,
                                    providerId = providerId,
                                    startTs = currentStart,
                                    endTs = currentEnd,
                                    title = currentTitle.orEmpty(),
                                    description = currentDesc
                                )
                            )
                        }
                        currentChannelId = null
                    }
                }
            }
            eventType = parser.next()
        }
    }

    private fun parseTime(value: String?): Long {
        if (value.isNullOrBlank()) return 0L
        for (format in DATE_FORMATS) {
            try {
                format.parse(value)?.let { return it.time }
            } catch (_: Exception) {
                // try next format
            }
        }
        return 0L
    }
}
