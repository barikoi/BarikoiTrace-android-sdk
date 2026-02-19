package com.barikoi.barikoitrace.network.dto;

import com.google.gson.annotations.SerializedName;

public class ActiveTripResponse {
    @SerializedName("active")
    private boolean active;

    @SerializedName("trip")
    private TripResponse.TripData trip;

    public boolean isActive() {
        return active;
    }

    public TripResponse.TripData getTrip() {
        return trip;
    }
}