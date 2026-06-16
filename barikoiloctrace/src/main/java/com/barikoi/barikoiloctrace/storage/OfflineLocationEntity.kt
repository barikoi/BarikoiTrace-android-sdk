package com.barikoi.barikoiloctrace.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_location")
data class OfflineLocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val json: String
)
