package com.barikoi.barikoitrace.network.dto;

import com.google.gson.annotations.SerializedName;

public class StartTripRequest {
    @SerializedName("api_key")
    private String apiKey;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("start_time")
    private String startTime;

    @SerializedName("tag")
    private String tag;

    @SerializedName("debug")
    private Boolean debug;

    public StartTripRequest(String apiKey, String userId, String startTime, String tag, Boolean debug) {
        this.apiKey = apiKey;
        this.userId = userId;
        this.startTime = startTime;
        this.tag = tag;
        this.debug = debug;
    }
}