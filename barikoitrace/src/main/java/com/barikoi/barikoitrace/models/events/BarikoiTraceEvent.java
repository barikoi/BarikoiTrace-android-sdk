package com.barikoi.barikoitrace.models.events;

import androidx.annotation.Keep;

@Keep
public class BarikoiTraceEvent {
    private String activity;
    private double altitude;
    private double course;
    private String created_at;
    private double distance;
    private String event_source;
    private String event_type;
    private String event_version;
    private String geofence_id;
    private double horizontal_accuracy;
    private Location location = new Location();
    private String location_id;
    private String nearby_user_id;
    private String recorded_at;
    private int speed;
    private String trip_id;
    private String user_id;
    private double vertical_accuracy;

    public String getActivity() {
        return this.activity;
    }

    public double getAltitude() {
        return this.altitude;
    }

    public double getCourse() {
        return this.course;
    }

    public String getCreated_at() {
        return this.created_at;
    }

    public double getDistance() {
        return this.distance;
    }

    public String getEvent_source() {
        return this.event_source;
    }

    public String getEvent_type() {
        return this.event_type;
    }

    public String getEvent_version() {
        return this.event_version;
    }

    public String getGeofence_id() {
        return this.geofence_id;
    }

    public double getHorizontal_accuracy() {
        return this.horizontal_accuracy;
    }

    public Location getLocation() {
        return this.location;
    }

    public String getLocation_id() {
        return this.location_id;
    }

    public String getNearby_user_id() {
        return this.nearby_user_id;
    }

    public String getRecorded_at() {
        return this.recorded_at;
    }

    public int getSpeed() {
        return this.speed;
    }

    public String getTrip_id() {
        return this.trip_id;
    }

    public String getUser_id() {
        return this.user_id;
    }

    public double getVertical_accuracy() {
        return this.vertical_accuracy;
    }

    public void setActivity(String str) {
        this.activity = str;
    }

    public void setAltitude(double d) {
        this.altitude = d;
    }

    public void setCourse(double d) {
        this.course = d;
    }

    public void setCreated_at(String str) {
        this.created_at = str;
    }

    public void setDistance(double d) {
        this.distance = d;
    }

    public void setEvent_source(String str) {
        this.event_source = str;
    }

    public void setEvent_type(String str) {
        this.event_type = str;
    }

    public void setEvent_version(String str) {
        this.event_version = str;
    }

    public void setGeofence_id(String str) {
        this.geofence_id = str;
    }

    public void setHorizontal_accuracy(double d) {
        this.horizontal_accuracy = d;
    }

    public void setLocation(Location location2) {
        this.location = location2;
    }

    public void setLocation_id(String str) {
        this.location_id = str;
    }

    public void setNearby_user_id(String str) {
        this.nearby_user_id = str;
    }

    public void setRecorded_at(String str) {
        this.recorded_at = str;
    }

    public void setSpeed(int i) {
        this.speed = i;
    }

    public void setTrip_id(String str) {
        this.trip_id = str;
    }

    public void setUser_id(String str) {
        this.user_id = str;
    }

    public void setVertical_accuracy(double d) {
        this.vertical_accuracy = d;
    }
}
