package com.barikoi.barikoitrace;

public class LocationUpdateConfig {

    private long interval=5000;
    private float smallestDistance=50;


    public LocationUpdateConfig( long interval, float smallestDistance){
        this.interval=interval;
        this.smallestDistance=smallestDistance;
    }

    public long getInterval() {
        return interval;
    }

    public float getSmallestDistance() {
        return smallestDistance;
    }
}
