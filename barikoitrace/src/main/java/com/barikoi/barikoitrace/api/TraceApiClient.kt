package com.barikoi.barikoitrace.api

import android.content.Context
import com.barikoi.barikoitrace.util.TraceLogger
import com.barikoi.barikoitrace.TraceMode
import com.barikoi.barikoitrace.model.TraceUser
import com.barikoi.barikoitrace.storage.TraceDataStore
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalTime

class TraceApiClient private constructor(context: Context) {

    private val dataStore = TraceDataStore(context)
    private var baseUrl: String
    private var apiKey: String? = null
    private var userId: String? = null

    private val logger = TraceLogger(context, "TraceApi")

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Content-Type", "application/json")
                .build()
            logger.d("HTTP request: ${request.method}")
            chain.proceed(request).also { logger.d("HTTP response: ${it.code}") }
        }
        .build()

    private var retrofit: Retrofit
    private var apiService: TraceApiService

    init {
        baseUrl = ensureTrailingSlash(dataStore.getBaseUrl() ?: ApiRoutes.BASE_URL)
        apiKey = dataStore.getApiKey()
        userId = dataStore.getUserId()
        retrofit = buildRetrofit(baseUrl)
        apiService = retrofit.create(TraceApiService::class.java)
    }

    private fun ensureTrailingSlash(url: String): String {
        return if (url.endsWith("/")) url else "$url/"
    }

    private fun buildUrl(route: String): String {
        val base = baseUrl.trimEnd('/')
        return base + route
    }

    private fun buildRetrofit(url: String): Retrofit {
        val safeUrl = ensureTrailingSlash(url.ifEmpty { ApiRoutes.BASE_URL })
        return Retrofit.Builder()
            .baseUrl(safeUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun setBaseUrl(url: String) {
        baseUrl = ensureTrailingSlash(url)
        retrofit = buildRetrofit(baseUrl)
        apiService = retrofit.create(TraceApiService::class.java)
    }

    fun setApiKey(key: String) { apiKey = key }
    fun setUserId(id: String) { userId = id }

    // --- API Methods ---

    suspend fun authenticate(name: String?, email: String?, phone: String): TraceUser {
        val body = JsonObject().apply {
            addProperty("api_key", apiKey)
            name?.let { addProperty("name", it) }
            email?.let { addProperty("email", it) }
            addProperty("phone", phone)
        }

        logger.d("API call: authenticate")
        val response = apiService.authenticate(buildUrl(ApiRoutes.AUTHENTICATE), body)
        val json = parseResponse(response)

        val userJson = json.getAsJsonObject("user")
        val id = userJson.get("_id").asString
        val companies = userJson.getAsJsonArray("companies")
        if (companies.size() == 0) throw Exception("Company not found")
        val company = companies[0].asJsonObject.get("company_id").asString
        val group = companies[0].asJsonObject.get("group_id").asString

        val user = TraceUser(
            userId = id, name = "", email = "",
            phone = phone, companyId = company, group = group
        )
        userId = user.userId
        dataStore.setUser(user)
        return user
    }

    suspend fun getCompanySettings(phone: String): TraceMode {
        val body = JsonObject().apply {
            addProperty("api_key", apiKey)
            addProperty("phone", phone)
        }

        logger.d("API call: getCompanySettings")
        val response = apiService.getCompanySettings(buildUrl(ApiRoutes.COMPANY_SETTINGS), body)
        val json = parseResponse(response)
        val settings = json.getAsJsonObject("settings")

        return TraceMode.Builder()
            .setUpdateInterval(settings.get("update_time_interval").asInt)
            .setDistanceFilter(settings.get("distance_interval").asInt)
            .setAccuracyFilter(settings.get("accuracy_filter").asInt)
            .setOfflineSync(settings.get("offline_sync").asBoolean)
            .setStartTime(LocalTime.parse(settings.get("tracking_start_time").asString))
            .setEndTime(LocalTime.parse(settings.get("tracking_end_time").asString))
            .build()
    }

    // --- Helpers ---

    private fun parseResponse(response: Response<JsonObject>): JsonObject {
        if (!response.isSuccessful || response.body() == null) {
            logger.e("API request failed (HTTP ${response.code()})")
            throw Exception("Server error: ${response.code()}")
        }
        logger.d("API response: ${response.code()}")
        return response.body()!!
    }

    companion object {
        @Volatile
        private var INSTANCE: TraceApiClient? = null

        fun getInstance(context: Context): TraceApiClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TraceApiClient(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
