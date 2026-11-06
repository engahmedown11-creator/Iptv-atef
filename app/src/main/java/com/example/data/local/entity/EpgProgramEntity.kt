package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "epg_programs",
    indices = [
        Index(value = ["channelEpgId", "startTimeUtc"])
    ]
)
data class EpgProgramEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val channelEpgId: String,
    val title: String,
    val description: String = "",
    val startTimeUtc: Long,
    val endTimeUtc: Long
)
