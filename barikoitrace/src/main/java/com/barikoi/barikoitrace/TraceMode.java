package com.barikoi.barikoitrace;

import androidx.annotation.Keep;

@Keep
public final class TraceMode {
    public static final TraceMode ACTIVE = new TraceMode(DesiredAccuracy.HIGH, 0, 0, 0, 50, TrackingModes.ACTIVE);
    public static final TraceMode PASSIVE = new TraceMode(DesiredAccuracy.HIGH, 0, 0, 0, 100, TrackingModes.PASSIVE);
    public static final TraceMode REACTIVE = new TraceMode(DesiredAccuracy.HIGH, 0, 0, 0, 75, TrackingModes.REACTIVE);
    private int accuracyFilter;
    private DesiredAccuracy desiredAccuracy;
    private int distanceFilter;
    private int stopDuration;
    private TrackingModes trackingModes;
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
        public Builder() {

        }

        public Builder(int distanceFilter, int updateInterval) {
            this.distanceFilter = distanceFilter;
            this.stopDuration = updateInterval;
        }
        public Builder setDistancefilter(int distanceFilter){
            this.distanceFilter=distanceFilter;
            return this;
        }
        public Builder setUpdateInterval(int updateInterval){
            this.updateInterval=updateInterval;
            return this;
        }
        public TraceMode build() {
            return new TraceMode(this.desiredAccuracy, this.updateInterval, this.distanceFilter, this.stopDuration, this.accuracyFilter, TrackingModes.CUSTOM);
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


    public enum TrackingModes {
        PASSIVE(0),
        REACTIVE(1),
        ACTIVE(2),
        CUSTOM(3);
        


        int option;

        private TrackingModes(int i) {
            this.option = i;
        }


        public int getOption() {
            return this.option;
        }
    }




    private TraceMode(DesiredAccuracy desiredAccuracy2, int updateinterval, int distancefilter, int stopduration, int accuracyfilter, TrackingModes trackingModes) {
        this.desiredAccuracy = desiredAccuracy2;
        this.updateInterval = updateinterval;
        this.distanceFilter = distancefilter;
        this.stopDuration = stopduration;
        this.accuracyFilter = accuracyfilter;
        this.trackingModes = trackingModes;
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

    public TrackingModes getTrackingModes() {
        return this.trackingModes;
    }

    public int getUpdateInterval() {
        return this.updateInterval;
    }
}
