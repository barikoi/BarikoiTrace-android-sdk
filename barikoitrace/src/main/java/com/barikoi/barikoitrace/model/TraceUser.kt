package com.barikoi.barikoitrace.model

data class TraceUser(
    val userId: String,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val companyId: String? = null,
    val group: String? = null,
    val lastLat: Double = 0.0,
    val lastLon: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
)
