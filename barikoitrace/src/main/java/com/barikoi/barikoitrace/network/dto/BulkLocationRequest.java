package com.barikoi.barikoitrace.network.dto;

import com.google.gson.JsonArray;
import com.google.gson.annotations.SerializedName;

public class BulkLocationRequest {
    @SerializedName("api_key")
    private String apiKey;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("gpx_bulk")
    private JsonArray gpxBulk;

    public BulkLocationRequest(String apiKey, String userId, JsonArray gpxBulk) {
        this.apiKey = apiKey;
        this.userId = userId;
        this.gpxBulk = gpxBulk;
    }
}
