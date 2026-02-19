package com.barikoi.barikoitrace.network.dto;

import com.google.gson.annotations.SerializedName;

public class AuthenticateRequest {
    @SerializedName("api_key")
    private String apiKey;

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("phone")
    private String phone;

    public AuthenticateRequest(String apiKey, String name, String email, String phone) {
        this.apiKey = apiKey;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }
}
