package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "series_episodes",
    indices = [
        Index(value = ["subscriptionId", "seriesStreamId", "seasonNumber", "episodeNumber"], unique = true)
    ]
)
data class EpisodeEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0,
    val subscriptionId: Long,
    val seriesStreamId: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val originalTitle: String,
    val arabicOverview: String = "",
    val streamUrl: String,
    val durationMinutes: Int = 0,
    val rating: String = "",
    val posterUrl: String = "",
    val resumePositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val lastWatchedTimestamp: Long = 0L
)
