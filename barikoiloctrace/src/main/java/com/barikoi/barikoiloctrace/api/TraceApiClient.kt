package com.barikoi.barikoiloctrace.api

import android.content.Context
import com.barikoi.barikoiloctrace.TraceMode
import com.barikoi.barikoiloctrace.model.TraceError
import com.barikoi.barikoiloctrace.model.TraceUser
import com.barikoi.barikoiloctrace.model.Trip
import com.barikoi.barikoiloctrace.storage.TraceDataStore
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalTime

class TraceApiClient private constructor(context: Context) {

    private val dataStore = TraceDataStore(context)
    private var baseUrl: String
    private var apiKey: String? = null
    private var userId: String? = null

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d("TraceApi", message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Content-Type", "application/json")
                .build()
            Log.d("TraceApi", "--> ${request.method} ${request.url}")
            chain.proceed(request)
        }
        .addInterceptor(loggingInterceptor)
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

        Log.d("TraceApi", "API CALL: authenticate | phone=$phone")
        val response = apiService.authenticate(buildUrl(ApiRoutes.AUTHENTICATE), body)
        val json = parseResponse(response)

        val userJson = json.getAsJsonObject("user")
        val id = userJson.get("_id").asString
        val userName = userJson.get("name").asString
        val userEmail = userJson.get("email").asString
        val companies = userJson.getAsJsonArray("companies")
        if (companies.size() == 0) throw Exception("Company not found")
        val company = companies[0].asJsonObject.get("company_id").asString
        val group = companies[0].asJsonObject.get("group_id").asString

        val user = TraceUser(
            userId = id, name = userName, email = userEmail,
            phone = phone, companyId = company, group = group
        )
        userId = user.userId
        dataStore.setUser(user)
        return user
    }

    suspend fun sendLocation(
        latitude: Double, longitude: Double, altitude: Double,
        bearing: Float, speed: Float, accuracy: Float, gpxTime: String
    ) {
        val body = JsonObject().apply {
            addProperty("api_key", apiKey)
            addProperty("user_id", userId)
            addProperty("latitude", latitude)
            addProperty("longitude", longitude)
            addProperty("altitude", altitude)
            addProperty("speed", speed)
            addProperty("bearing", bearing)
            addProperty("gpx_time", gpxTime)
            addProperty("accuracy", accuracy)
        }

        Log.d("TraceApi", "API CALL: sendLocation | lat=$latitude, lon=$longitude, time=$gpxTime")
        val response = apiService.sendLocation(buildUrl(ApiRoutes.ADD_GPX), body)
        val json = parseResponse(response)
        val status = json.get("status").asInt
        if (status != 200) {
            throw Exception(json.get("message").asString)
        }
    }

    suspend fun sendBulkLocations(data: JsonArray) {
        val body = JsonObject().apply {
            addProperty("api_key", apiKey)
            addProperty("user_id", userId)
            add("gpx_bulk", data)
        }

        Log.d("TraceApi", "API CALL: sendBulkLocations | count=${data.size()}")
        val response = apiService.sendBulkLocations(buildUrl(ApiRoutes.BULK_GPX), body)
        val json = parseResponse(response)
        val status = json.get("status").asInt
        if (status != 200) {
            throw Exception(json.get("message").asString)
        }
    }

    suspend fun startTrip(startTime: String, tag: String?, traceMode: TraceMode): Trip {
        val body = JsonObject().apply {
            addProperty("api_key", apiKey)
            addProperty("user_id", userId)
            addProperty("start_time", startTime)
            tag?.let { addProperty("tag", it) }
            if (traceMode.debug) addProperty("debug", true)
        }

        Log.d("TraceApi", "API CALL: startTrip | tag=$tag, startTime=$startTime")
        val response = apiService.startTrip(buildUrl(ApiRoutes.START_TRIP), body)
        val json = parseResponse(response)
        val status = json.get("status").asString
        if (status != "success") {
            throw Exception(json.get("error").asString)
        }

        val tripJson = json.getAsJsonObject("trip")
        return parseTrip(tripJson)
    }

    suspend fun endTrip(endTime: String): Trip {
        val body = JsonObject().apply {
            addProperty("api_key", apiKey)
            addProperty("user_id", userId)
            addProperty("end_time", endTime)
        }

        Log.d("TraceApi", "API CALL: endTrip | endTime=$endTime")
        val response = apiService.endTrip(buildUrl(ApiRoutes.END_TRIP), body)
        val json = parseResponse(response)
        val status = json.get("status").asString
        if (status != "success") {
            throw Exception(json.get("message").asString)
        }

        val tripJson = json.getAsJsonObject("trip")
        return parseTrip(tripJson)
    }

    suspend fun getActiveTrip(): Trip? {
        val params = mapOf(
            "api_key" to (apiKey ?: ""),
            "user_id" to (userId ?: "")
        )
        Log.d("TraceApi", "API CALL: getActiveTrip")
        val response = apiService.getActiveTrip(buildUrl(ApiRoutes.ACTIVE_TRIP), params)
        val json = parseResponse(response)
        val active = json.get("active").asBoolean
        if (!active) return null
        return parseTrip(json.getAsJsonObject("trip"))
    }

    suspend fun getCompanySettings(phone: String): TraceMode {
        val body = JsonObject().apply {
            addProperty("api_key", apiKey)
            addProperty("phone", phone)
        }

        Log.d("TraceApi", "API CALL: getCompanySettings | phone=$phone")
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
            Log.e("TraceApi", "API ERROR: ${response.code()} ${response.message()}")
            throw Exception("Server error: ${response.code()}")
        }
        Log.d("TraceApi", "API RESPONSE: ${response.code()} | ${response.body()}")
        return response.body()!!
    }

    private fun parseTrip(tripJson: JsonObject): Trip {
        val id = if (tripJson.has("_id")) tripJson.get("_id").asString else ""
        val startTime = tripJson.get("start_time").asString
        val state = if (tripJson.has("status")) tripJson.get("status").asInt
        else if (tripJson.has("state")) tripJson.get("state").asInt else 0
        val tag = if (tripJson.has("tag")) tripJson.get("tag").asString else null
        val endTime = if (tripJson.has("end_time") && !tripJson.get("end_time").isJsonNull)
            tripJson.get("end_time").asString else null

        return Trip(tripId = id, startTime = startTime, endTime = endTime, tag = tag, state = state)
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
