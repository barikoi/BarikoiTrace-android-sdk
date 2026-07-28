package com.barikoi.barikoitrace.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface OfflineLocationDao {

    @Insert
    suspend fun insert(entity: OfflineLocationEntity)

    @Query("SELECT COUNT(*) FROM offline_location")
    suspend fun getCount(): Int

    @Query("SELECT * FROM offline_location ORDER BY id ASC LIMIT 100")
    suspend fun getBatch(): List<OfflineLocationEntity>

    @Query("DELETE FROM offline_location WHERE id IN (SELECT id FROM offline_location ORDER BY id ASC LIMIT 100)")
    suspend fun deleteBatch(): Int
}
