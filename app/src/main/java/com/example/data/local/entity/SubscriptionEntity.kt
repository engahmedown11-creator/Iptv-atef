package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.security.CryptoManager

enum class SubscriptionType {
    M3U,
    XTREAM
}

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: SubscriptionType,
    val serverUrl: String,
    val username: String = "",
    val password: String = "", // Stored AES-256 encrypted
    val m3uUrl: String = "",
    val epgUrl: String = "",
    val isActive: Boolean = true,
    val expiryDate: String = "",
    val maxConnections: Int = 1,
    val totalChannels: Int = 0,
    val totalMovies: Int = 0,
    val totalSeries: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun getDecryptedPassword(): String {
        return CryptoManager.decrypt(password)
    }

    fun getDecryptedUsername(): String {
        return CryptoManager.decrypt(username)
    }
}
