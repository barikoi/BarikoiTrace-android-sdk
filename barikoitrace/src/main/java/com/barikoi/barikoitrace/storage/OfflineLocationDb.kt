package com.barikoi.barikoitrace.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [OfflineLocationEntity::class], version = 1, exportSchema = false)
abstract class OfflineLocationDb : RoomDatabase() {

    abstract fun locationDao(): OfflineLocationDao

    companion object {
        @Volatile
        private var INSTANCE: OfflineLocationDb? = null

        fun getInstance(context: Context): OfflineLocationDb {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    OfflineLocationDb::class.java,
                    "barikoi_trace_offline"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
