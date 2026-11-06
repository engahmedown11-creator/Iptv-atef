package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class VodType {
    MOVIE,
    SERIES
}

@Entity(
    tableName = "vod_items",
    indices = [
        Index(value = ["subscriptionId", "streamId", "type"], unique = true),
        Index(value = ["type", "categoryName"]),
        Index(value = ["isFavorite"])
    ]
)
data class VodItemEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0,
    val subscriptionId: Long,
    val streamId: String,
    val type: VodType,
    // Original title strictly preserved without translation
    val originalTitle: String,
    // Arabic metadata enriched
    val arabicOverview: String = "",
    val categoryName: String = "عام",
    val posterUrl: String = "",
    val backdropUrl: String = "",
    val releaseYear: String = "",
    val durationMinutes: Int = 0,
    val rating: String = "",
    val streamUrl: String = "",
    val extension: String = "mp4",
    val isFavorite: Boolean = false,
    val lastWatchedTimestamp: Long = 0L,
    val resumePositionMs: Long = 0L,
    val totalDurationMs: Long = 0L
)
