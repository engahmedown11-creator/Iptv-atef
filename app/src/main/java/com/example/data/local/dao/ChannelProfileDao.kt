package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ChannelProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelProfileDao {
    @Query("SELECT * FROM channel_profiles WHERE profileKey = :profileKey LIMIT 1")
    suspend fun getProfile(profileKey: String): ChannelProfileEntity?

    @Query("SELECT * FROM channel_profiles WHERE profileKey = :profileKey LIMIT 1")
    fun observeProfile(profileKey: String): Flow<ChannelProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: ChannelProfileEntity)

    @Query("DELETE FROM channel_profiles WHERE profileKey = :profileKey")
    suspend fun deleteProfile(profileKey: String)
}
