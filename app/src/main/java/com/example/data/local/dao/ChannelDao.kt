package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels WHERE subscriptionId = :subId ORDER BY localId ASC")
    fun getChannelsBySubscription(subId: Long): Flow<List<ChannelEntity>>

    @Query("SELECT DISTINCT categoryName FROM channels WHERE subscriptionId = :subId ORDER BY categoryName ASC")
    fun getCategoriesBySubscription(subId: Long): Flow<List<String>>

    @Query("SELECT * FROM channels WHERE subscriptionId = :subId AND categoryName = :category ORDER BY localId ASC")
    fun getChannelsByCategory(subId: Long, category: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE subscriptionId = :subId AND isFavorite = 1 ORDER BY localId ASC")
    fun getFavoriteChannels(subId: Long): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE subscriptionId = :subId AND lastWatchedTimestamp > 0 ORDER BY lastWatchedTimestamp DESC LIMIT 20")
    fun getRecentChannels(subId: Long): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE subscriptionId = :subId AND name LIKE '%' || :query || '%' ORDER BY localId ASC")
    fun searchChannels(subId: Long, query: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE localId = :localId")
    suspend fun getChannelById(localId: Long): ChannelEntity?

    @Query("SELECT * FROM channels WHERE subscriptionId = :subId AND channelId = :channelId LIMIT 1")
    suspend fun getChannelByStreamId(subId: Long, channelId: String): ChannelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Query("UPDATE channels SET isFavorite = :isFav WHERE localId = :localId")
    suspend fun setFavorite(localId: Long, isFav: Boolean)

    @Query("UPDATE channels SET lastWatchedTimestamp = :timestamp WHERE localId = :localId")
    suspend fun updateLastWatched(localId: Long, timestamp: Long)

    @Query("DELETE FROM channels WHERE subscriptionId = :subId")
    suspend fun deleteChannelsBySubscription(subId: Long)

    @Query("SELECT COUNT(*) FROM channels WHERE subscriptionId = :subId")
    suspend fun getChannelCount(subId: Long): Int
}
