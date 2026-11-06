package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.EpgProgramEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EpgDao {
    @Query("SELECT * FROM epg_programs WHERE channelEpgId = :epgId AND endTimeUtc >= :nowUtc ORDER BY startTimeUtc ASC LIMIT 5")
    fun getUpcomingPrograms(epgId: String, nowUtc: Long): Flow<List<EpgProgramEntity>>

    @Query("SELECT * FROM epg_programs WHERE channelEpgId = :epgId AND startTimeUtc <= :nowUtc AND endTimeUtc >= :nowUtc LIMIT 1")
    suspend fun getCurrentProgram(epgId: String, nowUtc: Long): EpgProgramEntity?

    @Query("SELECT * FROM epg_programs WHERE channelEpgId = :epgId AND startTimeUtc <= :nowUtc AND endTimeUtc >= :nowUtc LIMIT 1")
    fun observeCurrentProgram(epgId: String, nowUtc: Long): Flow<EpgProgramEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrograms(programs: List<EpgProgramEntity>)

    @Query("DELETE FROM epg_programs WHERE endTimeUtc < :thresholdUtc")
    suspend fun pruneOldPrograms(thresholdUtc: Long)

    @Query("DELETE FROM epg_programs WHERE channelEpgId = :epgId")
    suspend fun deleteProgramsForChannel(epgId: String)
}
