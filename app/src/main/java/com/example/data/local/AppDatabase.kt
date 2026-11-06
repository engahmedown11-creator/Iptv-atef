package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.ChannelDao
import com.example.data.local.dao.ChannelProfileDao
import com.example.data.local.dao.EpgDao
import com.example.data.local.dao.PlaybackHistoryDao
import com.example.data.local.dao.SubscriptionDao
import com.example.data.local.dao.VodDao
import com.example.data.local.entity.ChannelEntity
import com.example.data.local.entity.ChannelProfileEntity
import com.example.data.local.entity.EpgProgramEntity
import com.example.data.local.entity.EpisodeEntity
import com.example.data.local.entity.PlaybackHistoryEntity
import com.example.data.local.entity.SubscriptionEntity
import com.example.data.local.entity.VodItemEntity

@Database(
    entities = [
        SubscriptionEntity::class,
        ChannelEntity::class,
        ChannelProfileEntity::class,
        VodItemEntity::class,
        EpisodeEntity::class,
        PlaybackHistoryEntity::class,
        EpgProgramEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun channelDao(): ChannelDao
    abstract fun channelProfileDao(): ChannelProfileDao
    abstract fun vodDao(): VodDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    abstract fun epgDao(): EpgDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "atef_iptv.db"
                ).fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
