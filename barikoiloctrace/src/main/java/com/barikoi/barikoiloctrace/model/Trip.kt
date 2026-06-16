package com.barikoi.barikoiloctrace.model

data class Trip(
    val tripId: String,
    val startTime: String,
    val endTime: String? = null,
    val tag: String? = null,
    val state: Int = 0,
    val userId: String? = null,
    val synced: Int = 1
)
