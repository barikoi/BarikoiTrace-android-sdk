package com.barikoi.barikoitrace.models;

import android.location.Location;

import androidx.annotation.Keep;

@Keep
public class BarikoiTraceLocation {
    private String activity;
    private Location location;
    private String recordedAt;
    private String timezoneOffset;
    private String userId;

    public BarikoiTraceLocation(String str, Location location2, String str2, String str3, String str4) {
        this.userId = str;
        this.location = location2;
        this.activity = str2;
        this.recordedAt = str3;
        this.timezoneOffset = str4;
    }

    public String getActivity() {
        return this.activity;
    }

    public Location getLocation() {
        return this.location;
    }

    public String getRecordedAt() {
        return this.recordedAt;
    }

    public String getTimezoneOffset() {
        return this.timezoneOffset;
    }

    public String getUserId() {
        return this.userId;
    }
}
