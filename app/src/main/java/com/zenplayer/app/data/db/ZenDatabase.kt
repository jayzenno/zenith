package com.zenplayer.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProviderEntity::class, ChannelEntity::class, EpgProgramEntity::class],
    version = 1,
    exportSchema = true
)
abstract class ZenDatabase : RoomDatabase() {

    abstract fun providerDao(): ProviderDao
    abstract fun channelDao(): ChannelDao
    abstract fun epgDao(): EpgDao

    companion object {
        @Volatile
        private var instance: ZenDatabase? = null

        fun get(context: Context): ZenDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ZenDatabase::class.java,
                    "zenplayer.db"
                ).build().also { instance = it }
            }
        }
    }
}