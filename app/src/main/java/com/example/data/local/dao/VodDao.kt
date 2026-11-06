package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.EpisodeEntity
import com.example.data.local.entity.VodItemEntity
import com.example.data.local.entity.VodType
import kotlinx.coroutines.flow.Flow

@Dao
interface VodDao {
    // Movies & Series
    @Query("SELECT * FROM vod_items WHERE subscriptionId = :subId AND type = :type ORDER BY localId DESC")
    fun getVodItemsByType(subId: Long, type: VodType): Flow<List<VodItemEntity>>

    @Query("SELECT DISTINCT categoryName FROM vod_items WHERE subscriptionId = :subId AND type = :type ORDER BY categoryName ASC")
    fun getVodCategories(subId: Long, type: VodType): Flow<List<String>>

    @Query("SELECT * FROM vod_items WHERE subscriptionId = :subId AND type = :type AND categoryName = :category ORDER BY localId DESC")
    fun getVodByCategory(subId: Long, type: VodType, category: String): Flow<List<VodItemEntity>>

    @Query("SELECT * FROM vod_items WHERE subscriptionId = :subId AND type = :type AND isFavorite = 1 ORDER BY localId DESC")
    fun getFavoriteVod(subId: Long, type: VodType): Flow<List<VodItemEntity>>

    @Query("SELECT * FROM vod_items WHERE subscriptionId = :subId AND type = :type AND (originalTitle LIKE '%' || :query || '%' OR arabicOverview LIKE '%' || :query || '%') ORDER BY localId DESC")
    fun searchVod(subId: Long, type: VodType, query: String): Flow<List<VodItemEntity>>

    @Query("SELECT * FROM vod_items WHERE subscriptionId = :subId AND (originalTitle LIKE '%' || :query || '%' OR arabicOverview LIKE '%' || :query || '%') ORDER BY localId DESC")
    fun searchAllVod(subId: Long, query: String): Flow<List<VodItemEntity>>

    @Query("SELECT * FROM vod_items WHERE localId = :localId LIMIT 1")
    suspend fun getVodById(localId: Long): VodItemEntity?

    @Query("SELECT * FROM vod_items WHERE subscriptionId = :subId AND streamId = :streamId AND type = :type LIMIT 1")
    suspend fun getVodByStreamId(subId: Long, streamId: String, type: VodType): VodItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVodItems(items: List<VodItemEntity>)

    @Query("UPDATE vod_items SET isFavorite = :isFav WHERE localId = :localId")
    suspend fun setVodFavorite(localId: Long, isFav: Boolean)

    @Query("UPDATE vod_items SET arabicOverview = :overview WHERE localId = :localId")
    suspend fun updateVodOverview(localId: Long, overview: String)

    @Query("UPDATE vod_items SET resumePositionMs = :position, totalDurationMs = :duration, lastWatchedTimestamp = :timestamp WHERE localId = :localId")
    suspend fun updateVodResume(localId: Long, position: Long, duration: Long, timestamp: Long)

    @Query("DELETE FROM vod_items WHERE subscriptionId = :subId")
    suspend fun deleteVodBySubscription(subId: Long)

    @Query("SELECT COUNT(*) FROM vod_items WHERE subscriptionId = :subId AND type = :type")
    suspend fun getVodCount(subId: Long, type: VodType): Int

    // Episodes
    @Query("SELECT * FROM series_episodes WHERE subscriptionId = :subId AND seriesStreamId = :seriesStreamId ORDER BY seasonNumber ASC, episodeNumber ASC")
    fun getEpisodesForSeries(subId: Long, seriesStreamId: String): Flow<List<EpisodeEntity>>

    @Query("SELECT DISTINCT seasonNumber FROM series_episodes WHERE subscriptionId = :subId AND seriesStreamId = :seriesStreamId ORDER BY seasonNumber ASC")
    fun getSeasonsForSeries(subId: Long, seriesStreamId: String): Flow<List<Int>>

    @Query("SELECT * FROM series_episodes WHERE subscriptionId = :subId AND seriesStreamId = :seriesStreamId AND seasonNumber = :season ORDER BY episodeNumber ASC")
    fun getEpisodesBySeason(subId: Long, seriesStreamId: String, season: Int): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM series_episodes WHERE localId = :localId LIMIT 1")
    suspend fun getEpisodeById(localId: Long): EpisodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodes(episodes: List<EpisodeEntity>)

    @Query("UPDATE series_episodes SET resumePositionMs = :position, totalDurationMs = :duration, lastWatchedTimestamp = :timestamp WHERE localId = :localId")
    suspend fun updateEpisodeResume(localId: Long, position: Long, duration: Long, timestamp: Long)

    @Query("DELETE FROM series_episodes WHERE subscriptionId = :subId")
    suspend fun deleteEpisodesBySubscription(subId: Long)
}
