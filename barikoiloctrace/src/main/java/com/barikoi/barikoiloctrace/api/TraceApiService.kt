package com.barikoi.barikoiloctrace.api

import com.barikoi.barikoiloctrace.model.Trip
import com.barikoi.barikoiloctrace.TraceMode
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.*

interface TraceApiService {

    @POST
    suspend fun authenticate(@Url url: String, @Body body: JsonObject): Response<JsonObject>

    @POST
    suspend fun sendLocation(@Url url: String, @Body body: JsonObject): Response<JsonObject>

    @POST
    suspend fun sendBulkLocations(@Url url: String, @Body body: JsonObject): Response<JsonObject>

    @POST
    suspend fun startTrip(@Url url: String, @Body body: JsonObject): Response<JsonObject>

    @POST
    suspend fun endTrip(@Url url: String, @Body body: JsonObject): Response<JsonObject>

    @GET
    suspend fun getActiveTrip(@Url url: String, @QueryMap params: Map<String, String>): Response<JsonObject>

    @POST
    suspend fun getCompanySettings(@Url url: String, @Body body: JsonObject): Response<JsonObject>
}
