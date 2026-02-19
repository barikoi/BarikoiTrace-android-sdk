package com.barikoi.barikoitrace.network.dto;

import com.google.gson.annotations.SerializedName;

public class TripResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("trip")
    private TripData trip;

    @SerializedName("error")
    private String error;

    public String getStatus() {
        return status;
    }

    public TripData getTrip() {
        return trip;
    }

    public String getError() {
        return error;
    }

    public static class TripData {
        @SerializedName("_id")
        private String id;

        @SerializedName("start_time")
        private String startTime;

        @SerializedName("end_time")
        private String endTime;

        @SerializedName("tag")
        private String tag;

        @SerializedName("status")
        private int status;

        @SerializedName("user")
        private String userId;

        public String getId() {
            return id;
        }

        public String getStartTime() {
            return startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public String getTag() {
            return tag;
        }

        public int getStatus() {
            return status;
        }

        public String getUserId() {
            return userId;
        }
    }
}