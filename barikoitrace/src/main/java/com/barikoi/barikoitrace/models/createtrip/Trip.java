package com.barikoi.barikoitrace.models.createtrip;

public class Trip {


    String trip_id;
    String start_time;
    String end_time;
    String tag;
    int State;
    String user_id;

    public int getSynced() {
        return synced;
    }

    public void setSynced(int synced) {
        this.synced = synced;
    }

    int synced;

    public Trip(String trip_id, String start_time, String end_time, String tag, int state, String user_id, int synced) {
        this.trip_id=trip_id;
        this.start_time = start_time;
        this.end_time = end_time;
        this.tag = tag;
        this.State = state;
        this.user_id=user_id;
        this.synced=synced;
    }

    public String getStart_time() {
        return start_time;
    }

    public void setStart_time(String start_time) {
        this.start_time = start_time;
    }

    public String getEnd_time() {
        return end_time;
    }

    public void setEnd_time(String end_time) {
        this.end_time = end_time;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public int getState() {
        return State;
    }

    public void setState(int state) {
        State = state;
    }

    public String getTrip_id() {return trip_id;}

    public void setTrip_id(String trip_id) {this.trip_id = trip_id;}
}
