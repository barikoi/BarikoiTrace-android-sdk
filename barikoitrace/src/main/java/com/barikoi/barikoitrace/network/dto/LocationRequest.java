package com.barikoi.barikoitrace.network.dto;

import com.google.gson.annotations.SerializedName;

public class LocationRequest {
    @SerializedName("api_key")
    private String apiKey;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("altitude")
    private double altitude;

    @SerializedName("speed")
    private float speed;

    @SerializedName("bearing")
    private float bearing;

    @SerializedName("gpx_time")
    private String gpxTime;

    @SerializedName("accuracy")
    private float accuracy;

    @SerializedName("trip_id")
    private String tripId;

    @SerializedName("trip_status")
    private String tripStatus;

    public LocationRequest(String apiKey, String userId, double latitude, double longitude,
                          double altitude, float speed, float bearing, String gpxTime, float accuracy) {
        this.apiKey = apiKey;
        this.userId = userId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.speed = speed;
        this.bearing = bearing;
        this.gpxTime = gpxTime;
        this.accuracy = accuracy;
    }

    public LocationRequest(String apiKey, String userId, double latitude, double longitude,
                          double altitude, float speed, float bearing, String gpxTime,
                          float accuracy, String tripId, String tripStatus) {
        this(apiKey, userId, latitude, longitude, altitude, speed, bearing, gpxTime, accuracy);
        this.tripId = tripId;
        this.tripStatus = tripStatus;
    }
}
