package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "channels",
    indices = [
        Index(value = ["subscriptionId", "channelId"], unique = true),
        Index(value = ["categoryName"]),
        Index(value = ["isFavorite"])
    ]
)
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0,
    val subscriptionId: Long,
    val channelId: String, // Stream ID or unique M3U channel key
    val name: String,
    val logoUrl: String = "",
    val streamUrl: String,
    val categoryName: String = "عام",
    val epgChannelId: String = "",
    val isFavorite: Boolean = false,
    val lastWatchedTimestamp: Long = 0L,
    val streamType: String = "live" // live, hls, m3u8
)
