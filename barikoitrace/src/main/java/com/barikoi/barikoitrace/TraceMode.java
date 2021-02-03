package com.barikoi.barikoitrace;

import androidx.annotation.Keep;

@Keep
public final class TraceMode {
    public static final TraceMode ACTIVE = new TraceMode(DesiredAccuracy.HIGH, 0, 0, 0, 50, TrackingOptions.ACTIVE);
    public static final TraceMode PASSIVE = new TraceMode(DesiredAccuracy.HIGH, 0, 0, 0, 100, TrackingOptions.PASSIVE);
    public static final TraceMode REACTIVE = new TraceMode(DesiredAccuracy.HIGH, 0, 0, 0, 75, TrackingOptions.REACTIVE);
    private int accuracyFilter;
    private DesiredAccuracy desiredAccuracy;
    private int distanceFilter;
    private int stopDuration;
    private TrackingOptions trackingOptions;
    private int updateInterval;

    @Keep
    public enum AppState {
        FOREGROUND,
        BACKGROUND,
        ALWAYS_ON;

        public static AppState toEnum(String str) {
            return valueOf(str);
        }
    }

    @Keep
    public static class Builder {
        private int accuracyFilter = 100;
        private DesiredAccuracy desiredAccuracy = DesiredAccuracy.HIGH;
        private int distanceFilter = 0;
        private int stopDuration = 0;
        private int updateInterval = 0;
        public Builder(int updateInterval) {
            this.updateInterval = updateInterval;
        }

        public Builder(int distanceFilter, int updateInterval) {
            this.distanceFilter = distanceFilter;
            this.stopDuration = updateInterval;
        }

        public TraceMode build() {
            return new TraceMode(this.desiredAccuracy, this.updateInterval, this.distanceFilter, this.stopDuration, this.accuracyFilter, TrackingOptions.CUSTOM);
        }

        public Builder setAccuracyFilter(int i) {
            if (i < 20 || i > 150) {
                this.accuracyFilter = 100;
            } else {
                this.accuracyFilter = i;
            }
            return this;
        }

        public Builder setDesiredAccuracy(DesiredAccuracy desiredAccuracy2) {
            this.desiredAccuracy = desiredAccuracy2;
            return this;
        }
    }

    @Keep
    public enum DesiredAccuracy {
        HIGH,
        MEDIUM,
        LOW;

        public static DesiredAccuracy toEnum(String str) {
            return valueOf(str);
        }
    }


    public enum TrackingOptions {
        PASSIVE(0),
        REACTIVE(1),
        ACTIVE(2),
        CUSTOM(3);
        


        int option;

        private TrackingOptions(int i) {
            this.option = i;
        }


        public int getOption() {
            return this.option;
        }
    }




    private TraceMode(DesiredAccuracy desiredAccuracy2, int updateinterval, int distancefilter, int stopduration, int accuracyfilter, TrackingOptions trackingOptions) {
        this.desiredAccuracy = desiredAccuracy2;
        this.updateInterval = updateinterval;
        this.distanceFilter = distancefilter;
        this.stopDuration = stopduration;
        this.accuracyFilter = accuracyfilter;
        this.trackingOptions = trackingOptions;
    }

    public int getAccuracyFilter() {
        return this.accuracyFilter;
    }

    public String getDesiredAccuracy() {
        return this.desiredAccuracy.toString();
    }

    public int getDistanceFilter() {
        return this.distanceFilter;
    }

    public int getStopDuration() {
        return this.stopDuration;
    }

    public TrackingOptions getTrackingOptions() {
        return this.trackingOptions;
    }

    public int getUpdateInterval() {
        return this.updateInterval;
    }
}
