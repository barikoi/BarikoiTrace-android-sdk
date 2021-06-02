package com.barikoi.barikoitrace.models.createtrip;

public class Trip {


    int id;
    String start_time;
    String end_time;
    String tag;
    int State;
    int user_id;

    public int getId() {
        return id;
    }
    public int getSynced() {
        return synced;
    }

    public void setSynced(int synced) {
        this.synced = synced;
    }

    int synced;

    public Trip(int id, String start_time, String end_time, String tag, int state, int user_id, int synced) {
        this.id =id;
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
}
