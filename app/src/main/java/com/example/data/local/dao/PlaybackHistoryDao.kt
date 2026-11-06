package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.PlaybackHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackHistoryDao {
    @Query("SELECT * FROM playback_history WHERE subscriptionId = :subId ORDER BY lastWatchedTimestamp DESC LIMIT 20")
    fun getContinueWatching(subId: Long): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history WHERE historyKey = :key LIMIT 1")
    suspend fun getHistoryItem(key: String): PlaybackHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveHistory(item: PlaybackHistoryEntity)

    @Query("DELETE FROM playback_history WHERE historyKey = :key")
    suspend fun deleteHistoryItem(key: String)

    @Query("DELETE FROM playback_history WHERE subscriptionId = :subId")
    suspend fun clearHistoryForSubscription(subId: Long)
}
