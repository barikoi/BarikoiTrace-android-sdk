package com.barikoi.barikoitrace.models;

import androidx.annotation.Keep;

@Keep
public class BarikoiTraceUser {
    private String email;
    private String phone;
    private String userId;
    private double lastLat;
    private double lastLon;
    public BarikoiTraceUser(String userId, String phone,  String email) {
        this.userId = userId;
        this.phone = phone;
        this.email =email;
    }

    public BarikoiTraceUser(String userId, String phone,  String email, double lastLat, double latLon) {
        this.userId = userId;
        this.phone = phone;
        this.email =email;
        this.lastLat=lastLat;
        this.lastLon=lastLon;
    }



    public String getUserId() {
        return this.userId;
    }
}
