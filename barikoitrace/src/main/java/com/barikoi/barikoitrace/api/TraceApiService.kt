package com.barikoi.barikoitrace.api

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface TraceApiService {

    @POST
    suspend fun authenticate(@Url url: String, @Body body: JsonObject): Response<JsonObject>

    @POST
    suspend fun getCompanySettings(@Url url: String, @Body body: JsonObject): Response<JsonObject>
}
