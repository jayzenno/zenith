package com.zenplayer.app.data.parser

import com.zenplayer.app.data.model.Channel
import com.zenplayer.app.data.model.MediaType
import com.zenplayer.app.data.net.HttpEngine
import kotlin.math.absoluteValue

object M3uParser {

    private val ATTR_PATTERN = Regex("""([\w-]+)=(?:"([^"]*)"|'([^']*)'|([^\s"']+))""")
    private val EXTINF_PATTERN = Regex("""^#EXTINF:(-?\d+)\s*(.*)""")

    data class RawEntry(
        val name: String,
        val url: String,
        val logo: String?,
        val group: String?,
        val tvgId: String?,
        val duration: Int
    )

    private data class ExtinfLine(
        val duration: Int,
        val attrLine: String,
        val extgrp: String?
    )

    /**
     * Incremental parser: feed it lines one by one, it emits completed entries.
     * Keeps only a tiny amount of state so huge playlists do not need to be held in memory.
     */
    class StreamParser(
        private val baseUrl: String?,
        private val onEntry: (RawEntry) -> Unit
    ) {
        private var pending: ExtinfLine? = null
        private var pendingGroup: String? = null

        fun accept(rawLine: String) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) return
            if (trimmed.startsWith("#EXTM3U")) return

            if (trimmed.startsWith("#EXTGRP:", ignoreCase = true)) {
                val group = trimmed.substringAfter(":").trim().trim('"', '\'')
                pendingGroup = group.ifBlank { null }
                pending?.let { pending = it.copy(extgrp = pendingGroup) }
                return
            }

            val extinfMatch = EXTINF_PATTERN.matchEntire(trimmed)
            if (extinfMatch != null) {
                pending = parseExtinf(extinfMatch)
                return
            }

            if (trimmed.startsWith("#")) {
                pending = null
                return
            }

            val inf = pending ?: return
            val url = HttpEngine.resolve(baseUrl, trimmed)
            val (title, attrs) = parseAttributes(inf.attrLine)
            val group = attrs["group-title"]?.ifBlank { null }
                ?: inf.extgrp
                ?: pendingGroup
            onEntry(
                RawEntry(
                    name = attrs["tvg-name"]?.ifBlank { null } ?: title.ifBlank { inf.attrLine.trim() },
                    url = url,
                    logo = attrs["tvg-logo"]?.ifBlank { null }
                        ?: attrs["logo"]?.ifBlank { null },
                    group = group,
                    tvgId = attrs["tvg-id"]?.ifBlank { null },
                    duration = inf.duration
                )
            )
            pending = null
            pendingGroup = null
        }
    }

    fun parse(content: String, baseUrl: String? = null): List<RawEntry> {
        val results = ArrayList<RawEntry>()
        val parser = StreamParser(baseUrl) { results.add(it) }
        var start = 0
        val len = content.length
        while (start < len) {
            var end = content.indexOf('\n', start)
            if (end < 0) end = len
            val line = content.substring(start, if (end > start && content[end - 1] == '\r') end - 1 else end)
            parser.accept(line)
            start = end + 1
        }
        return results
    }

    private fun parseExtinf(match: MatchResult): ExtinfLine {
        val duration = match.groupValues[1].toIntOrNull() ?: -1
        val rest = match.groupValues[2]
        return ExtinfLine(duration, rest, null)
    }

    private fun parseAttributes(line: String): Pair<String, Map<String, String>> {
        val attrs = mutableMapOf<String, String>()
        var titleStart = -1
        var inQuotes = false
        var quoteChar = ' '

        for (i in line.indices) {
            val c = line[i]
            if (inQuotes) {
                if (c == quoteChar) inQuotes = false
                continue
            }
            if (c == '"' || c == '\'') {
                inQuotes = true
                quoteChar = c
                continue
            }
            if (c == ',') {
                titleStart = i
                break
            }
        }

        val attrPart = if (titleStart >= 0) line.substring(0, titleStart) else line
        val title = if (titleStart >= 0) line.substring(titleStart + 1).trim() else ""

        ATTR_PATTERN.findAll(attrPart).forEach { m ->
            val key = m.groupValues[1]
            val value = m.groupValues[2].ifEmpty { m.groupValues[3].ifEmpty { m.groupValues[4] } }
            if (key.isNotBlank()) attrs[key] = value
        }
        return title to attrs
    }

    fun classifyMediaType(entry: RawEntry): MediaType {
        val group = entry.group?.lowercase() ?: ""
        val name = entry.name.lowercase()
        val url = entry.url.lowercase()

        if (listOf("music", "radio", "audio", "musik", "webradio").any { group.contains(it) } ||
            listOf("radio", "webradio", "fm-", ".aac/", "stream.mp3", ".mp3", ".aac").any { url.contains(it) }
        ) {
            return MediaType.MUSIC
        }

        if (listOf("series", "serien", "episode", "folge", "staffel").any { group.contains(it) || name.contains(it) } ||
            listOf("/series/", "serien/", "season", "s01", "e01").any { url.contains(it) }
        ) {
            return MediaType.SERIES
        }

        if (listOf("vod", "movies", "film", "filme", "video", "kino", "cinema", "netflix", "disney", "amazon prime", "apple tv").any {
                group.contains(it) || name.contains(it)
            } ||
            listOf(".mkv", ".mp4", ".avi", ".m4v", "video-on-demand").any { url.contains(it) }
        ) {
            return MediaType.VOD
        }

        return MediaType.LIVE
    }

    fun channelId(providerId: Long, entry: RawEntry): String {
        val idHash = "${providerId}:${entry.url}:${entry.name}".hashCode().absoluteValue
        return "m3u_${providerId}_$idHash"
    }

    fun toChannels(
        entries: List<RawEntry>,
        providerId: Long
    ): List<Channel> {
        val counters = mutableMapOf<String, Int>()
        return entries.map { entry ->
            val type = classifyMediaType(entry)
            val counter = counters.getOrPut(type.name) { 0 } + 1
            counters[type.name] = counter
            Channel(
                id = channelId(providerId, entry),
                providerId = providerId,
                mediaType = type,
                name = entry.name,
                url = entry.url,
                logoUrl = entry.logo,
                category = entry.group,
                number = counter,
                extra = entry.tvgId
            )
        }
    }
}
