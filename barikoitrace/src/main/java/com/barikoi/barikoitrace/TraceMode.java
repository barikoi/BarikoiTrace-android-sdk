package com.barikoi.barikoitrace;

import androidx.annotation.Keep;

@Keep
public final class TraceMode {
    public static final TraceMode ACTIVE = new TraceMode(DesiredAccuracy.HIGH, 5, 0, 0, 50, TrackingModes.ACTIVE, true, false, 0);
    public static final TraceMode PASSIVE = new TraceMode(DesiredAccuracy.MEDIUM, 0, 100, 0, 300, TrackingModes.PASSIVE,true,false, 120);
    public static final TraceMode REACTIVE = new TraceMode(DesiredAccuracy.HIGH, 0, 100, 0, 100, TrackingModes.REACTIVE,true,false, 30);
    private int accuracyFilter;
    private DesiredAccuracy desiredAccuracy;
    private int distanceFilter;
    private int stopDuration;
    private TrackingModes trackingModes;
    private int updateInterval;
    private boolean offline;
    private boolean debug=false;
    private int pingSyncInterval= 0;

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
        private boolean offline=true;
        private boolean debug= false;
        private int pingSyncInterval= 0;
        public Builder() {

        }

        public Builder setDistancefilter(int distanceFilter){
            this.distanceFilter=distanceFilter;
            return this;
        }
        public Builder setUpdateInterval(int updateInterval){
            this.updateInterval=updateInterval;
            return this;
        }
        public Builder setOfflineSync(boolean offline){
            this.offline=offline;
            return this;
        }
        public Builder setDebugModeOn(){
            this.debug=true;
            return this;
        }
        public Builder setPingSyncInterval(int pingSyncInterval){
            this.pingSyncInterval=pingSyncInterval;
            return this;
        }
        public TraceMode build() {
            return new TraceMode(this.desiredAccuracy, this.updateInterval, this.distanceFilter, this.stopDuration, this.accuracyFilter, TrackingModes.CUSTOM,this.offline,this.debug, this.pingSyncInterval);
        }

        public Builder setAccuracyFilter(int i) {
            if (i < 10 || i > 150) {
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
            if(str.equals("") || str ==null) return HIGH;
            return valueOf(str);
        }
    }


    public enum TrackingModes {
        PASSIVE(0),
        REACTIVE(1),
        ACTIVE(2),
        CUSTOM(3);
        


        final int option;

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

    private TraceMode(DesiredAccuracy desiredAccuracy2, int updateinterval, int distancefilter, int stopduration, int accuracyfilter, TrackingModes trackingModes, boolean offline,boolean debug, int pingSyncInterval) {
        this.desiredAccuracy = desiredAccuracy2;
        this.updateInterval = updateinterval;
        this.distanceFilter = distancefilter;
        this.stopDuration = stopduration;
        this.accuracyFilter = accuracyfilter;
        this.trackingModes = trackingModes;
        this.offline=offline;
        this.debug=debug;
        this.pingSyncInterval=pingSyncInterval;
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

    public boolean isInDebugMode(){ return  this.debug;}

    public int getPingSyncInterval(){ return this.pingSyncInterval;}

    public String toString(){ return "TraceMode "+getTrackingModes()+", updateInterval: "
            +getUpdateInterval()+", distancefilter: "
            +getDistanceFilter()+", pingsyncinterval: "
            +getPingSyncInterval();
    }

}
