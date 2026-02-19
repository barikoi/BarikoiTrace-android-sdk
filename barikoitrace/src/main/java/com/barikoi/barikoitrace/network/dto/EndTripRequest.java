package com.barikoi.barikoitrace.network.dto;

import com.google.gson.annotations.SerializedName;

public class EndTripRequest {
    @SerializedName("api_key")
    private String apiKey;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("end_time")
    private String endTime;

    public EndTripRequest(String apiKey, String userId, String endTime) {
        this.apiKey = apiKey;
        this.userId = userId;
        this.endTime = endTime;
    }
}