package com.barikoi.barikoitrace;

public class LocationUpdateConfigBuilder {
    private long interval ;
    private float smallestDistance;

    public LocationUpdateConfigBuilder setInterval(long interval) {
        this.interval = interval;
        return this;
    }

    public LocationUpdateConfigBuilder setSmallestDistance(float smallestDistance) {
        this.smallestDistance = smallestDistance;
        return this;
    }

    public LocationUpdateConfig createLocationUpdateConfig() {
        return new LocationUpdateConfig(interval, smallestDistance);
    }
}