package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey
    val historyKey: String, // "movie_${vodLocalId}" or "ep_${episodeLocalId}"
    val subscriptionId: Long,
    val title: String,
    val subtitle: String = "", // e.g., "الموسم 1 - الحلقة 3"
    val posterUrl: String = "",
    val streamUrl: String,
    val contentType: String, // "MOVIE" or "SERIES_EPISODE"
    val referenceId: Long, // localId of VodItem or Episode
    val resumePositionMs: Long,
    val totalDurationMs: Long,
    val lastWatchedTimestamp: Long = System.currentTimeMillis()
)
