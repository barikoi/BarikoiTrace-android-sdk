package com.barikoi.barikoitrace.network.dto;

import com.google.gson.annotations.SerializedName;

public class CompanySettingsRequest {
    @SerializedName("api_key")
    private String apiKey;

    @SerializedName("phone")
    private String phone;

    public CompanySettingsRequest(String apiKey, String phone) {
        this.apiKey = apiKey;
        this.phone = phone;
    }
}