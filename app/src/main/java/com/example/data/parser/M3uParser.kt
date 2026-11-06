package com.example.data.parser

import com.example.data.local.entity.ChannelEntity
import com.example.data.local.entity.VodItemEntity
import com.example.data.local.entity.VodType
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.UUID

data class ParsedM3uResult(
    val channels: List<ChannelEntity>,
    val vodItems: List<VodItemEntity>
)

object M3uParser {
    fun parse(inputStream: InputStream, subscriptionId: Long): ParsedM3uResult {
        val channels = mutableListOf<ChannelEntity>()
        val vodItems = mutableListOf<VodItemEntity>()

        val reader = BufferedReader(InputStreamReader(inputStream))
        var line: String?

        var currentTvgId = ""
        var currentTvgName = ""
        var currentTvgLogo = ""
        var currentGroupTitle = "عام"
        var currentChannelName = ""

        while (reader.readLine().also { line = it } != null) {
            val trimmed = line!!.trim()
            if (trimmed.isEmpty()) continue

            if (trimmed.startsWith("#EXTINF:", ignoreCase = true)) {
                // Parse attributes
                currentTvgId = extractAttribute(trimmed, "tvg-id")
                currentTvgName = extractAttribute(trimmed, "tvg-name")
                currentTvgLogo = extractAttribute(trimmed, "tvg-logo")
                currentGroupTitle = extractAttribute(trimmed, "group-title").ifEmpty { "عام" }

                // The name is after the last comma
                val commaIndex = trimmed.lastIndexOf(',')
                currentChannelName = if (commaIndex != -1 && commaIndex < trimmed.length - 1) {
                    trimmed.substring(commaIndex + 1).trim()
                } else if (currentTvgName.isNotEmpty()) {
                    currentTvgName
                } else {
                    "قناة غير معنونة"
                }
            } else if (!trimmed.startsWith("#") && (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("rtsp://"))) {
                val streamUrl = trimmed
                val isVod = isVodUrl(streamUrl, currentGroupTitle)

                if (isVod) {
                    val streamId = UUID.nameUUIDFromBytes("$streamUrl$currentChannelName".toByteArray()).toString()
                    val isSeries = currentGroupTitle.contains("مسلسل", ignoreCase = true) ||
                            currentGroupTitle.contains("series", ignoreCase = true)

                    vodItems.add(
                        VodItemEntity(
                            subscriptionId = subscriptionId,
                            streamId = streamId,
                            type = if (isSeries) VodType.SERIES else VodType.MOVIE,
                            originalTitle = currentChannelName,
                            arabicOverview = "محتوى من تصنيف $currentGroupTitle ضمن اشتراكك.",
                            categoryName = currentGroupTitle,
                            posterUrl = currentTvgLogo,
                            streamUrl = streamUrl
                        )
                    )
                } else {
                    val channelId = if (currentTvgId.isNotEmpty()) {
                        currentTvgId
                    } else {
                        UUID.nameUUIDFromBytes("$streamUrl$currentChannelName".toByteArray()).toString()
                    }

                    channels.add(
                        ChannelEntity(
                            subscriptionId = subscriptionId,
                            channelId = channelId,
                            name = currentChannelName,
                            logoUrl = currentTvgLogo,
                            streamUrl = streamUrl,
                            categoryName = currentGroupTitle,
                            epgChannelId = currentTvgId
                        )
                    )
                }

                // Reset temporary fields
                currentTvgId = ""
                currentTvgName = ""
                currentTvgLogo = ""
                currentGroupTitle = "عام"
                currentChannelName = ""
            }
        }

        return ParsedM3uResult(channels, vodItems)
    }

    private fun extractAttribute(line: String, attribute: String): String {
        val pattern = """$attribute="([^"]*)"""".toRegex(RegexOption.IGNORE_CASE)
        val match = pattern.find(line)
        if (match != null) {
            return match.groupValues[1].trim()
        }
        val unquotedPattern = """$attribute=([^,\s]+)""".toRegex(RegexOption.IGNORE_CASE)
        val unquotedMatch = unquotedPattern.find(line)
        return unquotedMatch?.groupValues?.get(1)?.trim() ?: ""
    }

    private fun isVodUrl(url: String, groupTitle: String): Boolean {
        val lowerUrl = url.lowercase()
        val lowerGroup = groupTitle.lowercase()

        if (lowerGroup.contains("movie") || lowerGroup.contains("فيلم") || lowerGroup.contains("أفلام") ||
            lowerGroup.contains("series") || lowerGroup.contains("مسلسل") || lowerGroup.contains("مسلسلات")) {
            return true
        }

        return lowerUrl.endsWith(".mp4") || lowerUrl.endsWith(".mkv") || lowerUrl.endsWith(".avi")
    }
}
