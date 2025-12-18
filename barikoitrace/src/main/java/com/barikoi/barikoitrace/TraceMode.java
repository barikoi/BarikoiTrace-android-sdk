package com.barikoi.barikoitrace;

import androidx.annotation.Keep;

import java.time.LocalTime;

@Keep
public final class TraceMode {
    public static final TraceMode ACTIVE = new TraceMode(DesiredAccuracy.HIGH, 5, 0, 0, 50, TrackingModes.ACTIVE, true, false, 0, LocalTime.MIN, LocalTime.MAX);
    public static final TraceMode PASSIVE = new TraceMode(DesiredAccuracy.MEDIUM, 0, 100, 0, 300, TrackingModes.PASSIVE,true,false, 120, LocalTime.MIN, LocalTime.MAX);
    public static final TraceMode REACTIVE = new TraceMode(DesiredAccuracy.HIGH, 0, 100, 0, 100, TrackingModes.REACTIVE,true,false, 30, LocalTime.MIN, LocalTime.MAX);
    private int accuracyFilter;
    private DesiredAccuracy desiredAccuracy;
    private int distanceFilter;
    private int stopDuration;
    private TrackingModes trackingModes;
    private int updateInterval;
    private boolean offline;
    private boolean debug=false;
    private LocalTime startTime;
    private LocalTime endTime;
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
        private LocalTime startTime= LocalTime.MIN;
        private LocalTime endTime= LocalTime.MAX;

        public Builder() {

        }

        public Builder setDistancefilter(int distanceFilter){
            if (distanceFilter< 10)
                this.distanceFilter =10;
            else
                this.distanceFilter=distanceFilter;
            return this;
        }
        public Builder setUpdateInterval(int updateInterval){
            if( updateInterval < 5 )
                this.updateInterval=5;
            else
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

        public Builder setStartTime(LocalTime startTime){
            this.startTime=startTime;
            return this;

        }
        public Builder setEndTime(LocalTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public TraceMode build() {
            return new TraceMode(this.desiredAccuracy, this.updateInterval, this.distanceFilter, this.stopDuration, this.accuracyFilter, TrackingModes.CUSTOM,this.offline,this.debug, this.pingSyncInterval, this.startTime,this.endTime);
        }

        public Builder setAccuracyFilter(int i) {
            if (i < 20 ) {
                this.accuracyFilter = 20;
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

    private TraceMode(DesiredAccuracy desiredAccuracy2, int updateinterval, int distancefilter, int stopduration, int accuracyfilter, TrackingModes trackingModes, boolean offline,boolean debug, int pingSyncInterval, LocalTime startTime, LocalTime endTime) {
        this.desiredAccuracy = desiredAccuracy2;
        this.updateInterval = updateinterval;
        this.distanceFilter = distancefilter;
        this.stopDuration = stopduration;
        this.accuracyFilter = accuracyfilter;
        this.trackingModes = trackingModes;
        this.startTime = startTime;
        this.endTime =  endTime;
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

    public LocalTime getStartTime(){ return this.startTime;}

    public LocalTime getEndTime(){ return this.endTime;}

    public String toString(){ return "TraceMode "+getTrackingModes()+", updateInterval: "
            +getUpdateInterval()+", distancefilter: "
            +getDistanceFilter()+", pingsyncinterval: "
            +getPingSyncInterval();
    }

}
