package com.barikoi.barikoitrace.models;

import androidx.annotation.Keep;

import com.barikoi.barikoitrace.models.createtrip.Coordinates;


@Keep
public class BarikoiTraceLocationInfo {
    private String activity;
    private double altitude = 0.0d;
    private Coordinates coordinates;
    private double course = 0.0d;
    private String event_source;
    private String event_type;
    private String event_version;
    private double horizontal_accuracy = 0.0d;
    private String location_id;
    private String recorded_at;
    private int speed;
    private String user_id;
    private double vertical_accuracy = 0.0d;

    public String getActivity() {
        return this.activity;
    }

    public double getAltitude() {
        return this.altitude;
    }

    public Coordinates getCoordinates() {
        return this.coordinates;
    }

    public double getCourse() {
        return this.course;
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

    public double getHorizontal_accuracy() {
        return this.horizontal_accuracy;
    }

    public String getLocation_id() {
        return this.location_id;
    }

    public String getRecorded_at() {
        return this.recorded_at;
    }

    public int getSpeed() {
        return this.speed;
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

    public void setCoordinates(Coordinates coordinates2) {
        this.coordinates = coordinates2;
    }

    public void setCourse(double d) {
        this.course = d;
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

    public void setHorizontal_accuracy(double d) {
        this.horizontal_accuracy = d;
    }

    public void setLocation_id(String str) {
        this.location_id = str;
    }

    public void setRecorded_at(String str) {
        this.recorded_at = str;
    }

    public void setSpeed(int i) {
        this.speed = i;
    }

    public void setUser_id(String str) {
        this.user_id = str;
    }

    public void setVertical_accuracy(double d) {
        this.vertical_accuracy = d;
    }
}
