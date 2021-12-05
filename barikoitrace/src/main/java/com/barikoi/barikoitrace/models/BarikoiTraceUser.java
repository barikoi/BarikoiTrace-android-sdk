package com.barikoi.barikoitrace.models;

import androidx.annotation.Keep;

@Keep
public class BarikoiTraceUser {
    private String email=null;
    private String phone;
    private String userId;
    private double lastLat;
    private double lastLon;
    private String group;
    private BarikoiTraceUser(){

    }
    public BarikoiTraceUser(String userId, String phone,  String email) {
        this.userId = userId;
        this.phone = phone;
        this.email =email;
    }

    public BarikoiTraceUser(String userId, String phone,  String email, double lastLat, double lastLon) {
        this.userId = userId;
        this.phone = phone;
        this.email =email;
        this.lastLat=lastLat;
        this.lastLon=lastLon;
    }

    public BarikoiTraceUser(Builder builder) {
        this.userId = builder.userId;
        this.phone = builder.phone;
        this.email =builder.email;
        this.lastLat=builder.lastLat;
        this.lastLon=builder.lastLon;
        this.group=builder.group;
    }

    private static class Builder{
        private String email;
        private String phone;
        private String userId;
        private double lastLat;
        private double lastLon;
        private String group;

        public Builder(){

        }
        public Builder setphone(String phone){
            this.phone=phone;
            return this;
        }
        public Builder setUserId(String userId){
            this.userId=userId;
            return this;
        }public Builder setlastLat(double lastLat){
            this.lastLat=lastLat;
            return this;
        }public Builder setlastLon(double lastLon){
            this.lastLon=lastLon;
            return this;
        }public Builder setGroup(String group){
            this.group=group;
            return this;
        }public Builder setEmail(String email){
            this.email=email;
            return this;
        }
        public BarikoiTraceUser build(){
            return new BarikoiTraceUser(this);
        }
    }


    public String getUserId() {
        return this.userId;
    }
}
