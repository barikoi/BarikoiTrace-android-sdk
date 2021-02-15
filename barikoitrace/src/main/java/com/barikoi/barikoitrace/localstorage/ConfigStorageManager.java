package com.barikoi.barikoitrace.localstorage;

import android.content.Context;
import android.location.Location;
import android.text.TextUtils;

import com.barikoi.barikoitrace.TraceMode;
import com.barikoi.barikoitrace.exceptions.BarikoiTraceLogView;

import java.util.UUID;


public final class ConfigStorageManager {


    private static ConfigStorageManager INSTANCE;


    private SharedPRefHelper sharedPRefHelper;

    private ConfigStorageManager(Context context) {
        this.sharedPRefHelper = new SharedPRefHelper(context);
    }


    public static synchronized ConfigStorageManager getInstance(Context context) {
        ConfigStorageManager aVar;
        synchronized (ConfigStorageManager.class) {
            if (INSTANCE == null) {
                INSTANCE = new ConfigStorageManager(context);
            }
            aVar = INSTANCE;
        }
        return aVar;
    }


    private void updateTrackingModetoDB(TraceMode traceTrackingMode) {
        clearTrackingModefromDB();
        this.sharedPRefHelper.putString("desiredAccuracy", traceTrackingMode.getDesiredAccuracy());
        this.sharedPRefHelper.putInt("updateInterval", traceTrackingMode.getUpdateInterval());
        this.sharedPRefHelper.putInt("distanceFilter", traceTrackingMode.getDistanceFilter());
        this.sharedPRefHelper.putInt("stopDuration", traceTrackingMode.getStopDuration());
        this.sharedPRefHelper.putInt("accuracyFilter", traceTrackingMode.getAccuracyFilter());
        this.sharedPRefHelper.putInt("type", traceTrackingMode.getTrackingModes().getOption());
        //BarikoiTraceLogView.debugLog("desiredAccuracy: " +this.getDesiredAccuracy()+", updateInterval: "+this.getUpdateInterval()+",distanceFilter:"+this.getDistanceFilter()+",accuracyFilter:"+getAccuracyFilter());
    }



    private void clearTrackingModefromDB() {
        this.sharedPRefHelper.remove("desiredAccuracy");
        this.sharedPRefHelper.remove("updateInterval");
        this.sharedPRefHelper.remove("distanceFilter");
        this.sharedPRefHelper.remove("stopDuration");
        this.sharedPRefHelper.remove("accuracyFilter");
        this.sharedPRefHelper.remove("type");
    }


    public String getApiKey() {
        return this.sharedPRefHelper.getString("api_key");
    }


    public int getStopDuration() {
        return this.sharedPRefHelper.getInt("stopDuration");
    }



    public String getOauth() {
        return this.sharedPRefHelper.getString("oauth");
    }


    public TraceMode.AppState getAppstate() {
        return TraceMode.AppState.toEnum(this.sharedPRefHelper.getString("appTrackingState"));
    }


    public boolean getTripEvents() {
        return this.sharedPRefHelper.getBoolean("trips_events");
    }


    public String getSubscribeTrip() {
        return this.sharedPRefHelper.getString("subscribe_trip");
    }


    public int getType() {
        return this.sharedPRefHelper.getInt("type");
    }


    public int getUpdateInterval() {
        return this.sharedPRefHelper.getInt("updateInterval");
    }


    public String getUserID() {
        return this.sharedPRefHelper.getString("user_id");
    }


    public String getUUID() {
        if (TextUtils.isEmpty(this.sharedPRefHelper.getString("uuid"))) {
            this.sharedPRefHelper.putString("uuid", UUID.randomUUID().toString());
        }
        return this.sharedPRefHelper.getString("uuid");
    }




    public boolean getGPSCheck() {
        return this.sharedPRefHelper.getBoolean("gpsCheck");
    }




    public boolean isLogging() {
        return this.sharedPRefHelper.getBoolean("logger");
    }



    public boolean isOfflineTracking() {
        return !this.sharedPRefHelper.getBoolean("offlineTracking");
    }

    public void turnTrackingOn(TraceMode traceMode) {
        this.sharedPRefHelper.putBoolean("sdk_tracking", true);
        updateTrackingModetoDB(traceMode);
    }

    public boolean isSdkTracking() {
        return this.sharedPRefHelper.getBoolean("sdk_tracking");
    }




    public void stopSdkTracking() {
        this.sharedPRefHelper.putBoolean("sdk_tracking", false);
        clearTrackingModefromDB();
    }




    public void updateBattery(long j) {
        this.sharedPRefHelper.putLong("batteryPercentage", j);
    }


    public void updateLastLocation(Location location) {
        this.sharedPRefHelper.putString("latitude", String.valueOf(location.getLatitude()));
        this.sharedPRefHelper.putString("longitude", String.valueOf(location.getLongitude()));
        this.sharedPRefHelper.putString("time", String.valueOf(location.getTime()));
        this.sharedPRefHelper.putString("speed", String.valueOf(location.getSpeed()));
    }


    public void updateAppTrackingState(TraceMode.AppState appState) {
        this.sharedPRefHelper.putString("appTrackingState", appState.toString());
    }




    public void updateAccountID(String str) {
        this.sharedPRefHelper.putString("account_id", str);
    }





    public void setTripTrackingOff() {
        this.sharedPRefHelper.putBoolean("trip_tracking", false);
    }





    public void setLogging(boolean z) {
        this.sharedPRefHelper.putBoolean("logger", z);
    }


    public void m235c() {
        this.sharedPRefHelper.remove("latitude");
        this.sharedPRefHelper.remove("longitude");
        this.sharedPRefHelper.remove("time");
        this.sharedPRefHelper.remove("speed");
    }


    public void setAppstate(String str) {
        this.sharedPRefHelper.putString("appState", str);
    }

    public void m238d() {
        this.sharedPRefHelper.remove("oauth");
        this.sharedPRefHelper.remove("user_id");
        this.sharedPRefHelper.remove("app_id");
        this.sharedPRefHelper.remove("project_id");
        this.sharedPRefHelper.remove("account_id");
        this.sharedPRefHelper.remove("geofence_events");
        this.sharedPRefHelper.remove("trips_events");
        this.sharedPRefHelper.remove("location_events");
        this.sharedPRefHelper.remove("nearby_events");
        this.sharedPRefHelper.remove("event_listener");
        this.sharedPRefHelper.remove("location_listener");
        this.sharedPRefHelper.remove("subscribe_events");
        this.sharedPRefHelper.remove("subscribe_location");
        this.sharedPRefHelper.remove("subscribe_user");
        this.sharedPRefHelper.remove("tripUpdating");
        this.sharedPRefHelper.remove("locationCount");
        this.sharedPRefHelper.remove("locationCountStartedAt");
        //m222a();
    }


    public void setBatteryState(String str) {
        this.sharedPRefHelper.putString("batteryState", str);
    }



    public String m241e() {
        return this.sharedPRefHelper.getString("account_id");
    }


    public void m242e(String str) {
        this.sharedPRefHelper.putString("device_token", str);
    }


    public void setAccuracyEngine(boolean z) {
        this.sharedPRefHelper.putBoolean("accuracyEngine", z);
    }




    public boolean getAccuracyEngine() {
        return this.sharedPRefHelper.getBoolean("accuracyEngine");
    }


    public int getAccuracyFilter() {
        return this.sharedPRefHelper.getInt("accuracyFilter");
    }


    public void m248g(String str) {
        this.sharedPRefHelper.putString("project_id", str);
    }



    public void setApiKey(String str) {
        this.sharedPRefHelper.putString("api_key", str);
    }


    public void m252h(boolean z) {
        this.sharedPRefHelper.putBoolean("gpsCheck", z);
    }


    public String getAppState() {
        return this.sharedPRefHelper.getString("appState");
    }





    public String getBaseUrl() {
        return this.sharedPRefHelper.getString("baseurl");
    }




    public void isLocationCountSynced(boolean z) {
        this.sharedPRefHelper.putBoolean("locationCountSync", z);
    }


    public long getBattery() {
        return this.sharedPRefHelper.getLong("batteryPercentage");
    }


    public void setUserID(String str) {
        this.sharedPRefHelper.putString("user_id", str);
    }



    public String getBatteryState() {
        return TextUtils.isEmpty(this.sharedPRefHelper.getString("batteryState")) ? "unknown" : this.sharedPRefHelper.getString("batteryState");
    }




    public String getDesiredAccuracy() {
        return this.sharedPRefHelper.getString("desiredAccuracy");
    }



    public String getDeviceToken() {
        return this.sharedPRefHelper.getString("device_token");
    }



    public int getDistanceFilter() {
        return this.sharedPRefHelper.getInt("distanceFilter");
    }



    public void m270p(boolean z) {
        this.sharedPRefHelper.putBoolean("offlineTracking", z);
    }



    public void m272q(boolean z) {
        this.sharedPRefHelper.putBoolean("mqttUpdating", z);
    }




    public Location getLastLocation() {
        Location location = new Location("center");
        String d = this.sharedPRefHelper.getString("latitude");
        String d2 = this.sharedPRefHelper.getString("longitude");
        String d3 = this.sharedPRefHelper.getString("time");
        String d4 = this.sharedPRefHelper.getString("speed");
        if (!(d == null || d2 == null || d3 == null || d4 == null)) {
            location.setLatitude(Double.parseDouble(d));
            location.setLongitude(Double.parseDouble(d2));
            location.setTime(Long.parseLong(d3));
            location.setSpeed((float) Math.round(Double.parseDouble(d4)));
        }
        return location;
    }



    public int m278t() {
        return this.sharedPRefHelper.getInt("locationCount");
    }






}
