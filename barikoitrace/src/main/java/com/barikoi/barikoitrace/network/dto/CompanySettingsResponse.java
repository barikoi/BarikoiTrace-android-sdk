package com.barikoi.barikoitrace.network.dto;

import com.google.gson.annotations.SerializedName;

public class CompanySettingsResponse {
    @SerializedName("settings")
    private SettingsData settings;

    public SettingsData getSettings() {
        return settings;
    }

    public static class SettingsData {
        @SerializedName("update_time_interval")
        private int updateTimeInterval;

        @SerializedName("distance_interval")
        private int distanceInterval;

        @SerializedName("accuracy_filter")
        private int accuracyFilter;

        @SerializedName("offline_sync")
        private boolean offlineSync;

        @SerializedName("tracking_start_time")
        private String trackingStartTime;

        @SerializedName("tracking_end_time")
        private String trackingEndTime;

        public int getUpdateTimeInterval() {
            return updateTimeInterval;
        }

        public int getDistanceInterval() {
            return distanceInterval;
        }

        public int getAccuracyFilter() {
            return accuracyFilter;
        }

        public boolean isOfflineSync() {
            return offlineSync;
        }

        public String getTrackingStartTime() {
            return trackingStartTime;
        }

        public String getTrackingEndTime() {
            return trackingEndTime;
        }
    }
}