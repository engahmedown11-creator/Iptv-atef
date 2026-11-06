package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channel_profiles")
data class ChannelProfileEntity(
    @PrimaryKey
    val profileKey: String, // format: "sub_${subscriptionId}_ch_${channelId}"
    val aspectRatio: String = "AUTO", // AUTO, ORIGINAL, FIT, FILL, STRETCH, ZOOM, CROP, ASPECT_16_9, ASPECT_4_3, ASPECT_21_9, ASPECT_1_1
    val preferredQuality: String = "AUTO",
    val audioTrackIndex: Int = -1,
    val subtitleTrackIndex: Int = -1,
    val subtitleSizeSp: Float = 18f,
    val subtitleColorHex: String = "#FFFFFF",
    val subtitleBackgroundHex: String = "#80000000",
    val subtitleOffsetMs: Long = 0L,
    val subtitlePosition: String = "BOTTOM", // BOTTOM, TOP, MIDDLE
    val volumeLevel: Float = 1.0f,
    val lastUpdated: Long = System.currentTimeMillis()
)
