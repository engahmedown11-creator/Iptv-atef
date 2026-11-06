package com.example.data.parser

import android.util.Xml
import com.example.data.local.entity.EpgProgramEntity
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object EpgParser {
    private val dateFormat = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun parse(inputStream: InputStream): List<EpgProgramEntity> {
        val programs = mutableListOf<EpgProgramEntity>()
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)

            var eventType = parser.eventType
            var currentChannelId: String? = null
            var currentStart: Long = 0L
            var currentEnd: Long = 0L
            var currentTitle: String = ""
            var currentDesc: String = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if ("programme".equals(tagName, ignoreCase = true)) {
                            currentChannelId = parser.getAttributeValue(null, "channel")
                            val startStr = parser.getAttributeValue(null, "start")
                            val stopStr = parser.getAttributeValue(null, "stop")
                            currentStart = parseDate(startStr)
                            currentEnd = parseDate(stopStr)
                            currentTitle = ""
                            currentDesc = ""
                        } else if ("title".equals(tagName, ignoreCase = true)) {
                            currentTitle = parser.nextText().trim()
                        } else if ("desc".equals(tagName, ignoreCase = true)) {
                            currentDesc = parser.nextText().trim()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if ("programme".equals(tagName, ignoreCase = true)) {
                            if (!currentChannelId.isNullOrEmpty() && currentTitle.isNotEmpty()) {
                                programs.add(
                                    EpgProgramEntity(
                                        channelEpgId = currentChannelId,
                                        title = currentTitle,
                                        description = currentDesc,
                                        startTimeUtc = currentStart,
                                        endTimeUtc = currentEnd
                                    )
                                )
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) { }
        return programs
    }

    private fun parseDate(dateStr: String?): Long {
        if (dateStr.isNullOrEmpty()) return 0L
        return try {
            val sanitized = if (!dateStr.contains(" ")) {
                val base = dateStr.take(14)
                val tz = dateStr.drop(14).trim().ifEmpty { "+0000" }
                "$base $tz"
            } else {
                dateStr
            }
            dateFormat.parse(sanitized)?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
    }
}
