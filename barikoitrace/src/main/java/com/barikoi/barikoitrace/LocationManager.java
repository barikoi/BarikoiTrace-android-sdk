package com.barikoi.barikoitrace;

import android.app.Application;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;


import com.barikoi.barikoitrace.Utils.NetworkChecker;
import com.barikoi.barikoitrace.Utils.SystemSettingsManager;
import com.barikoi.barikoitrace.callback.BarikoiTraceGetTripCallback;
import com.barikoi.barikoitrace.callback.BarikoiTraceLocationUpdateCallback;
import com.barikoi.barikoitrace.callback.BarikoiTraceSettingsCallback;
import com.barikoi.barikoitrace.callback.BarikoiTraceTripStateCallback;
import com.barikoi.barikoitrace.callback.BarikoiTraceUserCallback;
import com.barikoi.barikoitrace.exceptions.BarikoiTraceException;
import com.barikoi.barikoitrace.exceptions.BarikoiTraceLogView;
import com.barikoi.barikoitrace.localstorage.ConfigStorageManager;
import com.barikoi.barikoitrace.localstorage.sqlitedb.LogDbHelper;
import com.barikoi.barikoitrace.models.BarikoiTraceError;
import com.barikoi.barikoitrace.models.BarikoiTraceErrors;
import com.barikoi.barikoitrace.models.BarikoiTraceUser;
import com.barikoi.barikoitrace.models.createtrip.Trip;
import com.barikoi.barikoitrace.network.ApiRequestManager;
import com.barikoi.barikoitrace.p000b.LocationTracker;
import com.barikoi.barikoitrace.p000b.p001c.ApplicationBinder;
import com.barikoi.barikoitrace.p000b.p002d.LocationUpdateListener;

import java.util.ArrayList;
import java.util.Map;


public final class LocationManager {


    private static LocationManager INSTANCE;


    private Context context;


    private ApiRequestManager apiRequestManager;


    private LocationTracker locationTracker;


    private ConfigStorageManager confdb;




    private LocationManager(Context context) {
        this.context = context.getApplicationContext();
        this.confdb = ConfigStorageManager.getInstance(context);
        this.apiRequestManager = ApiRequestManager.getInstance(context);
        this.locationTracker = new LocationTracker(context);
    }


    public static synchronized LocationManager getInstance(Context context) {
        LocationManager aVar;
        synchronized (LocationManager.class) {
            if (INSTANCE == null) {
                INSTANCE = new LocationManager(context);
            }
            aVar = INSTANCE;
        }
        return aVar;
    }





    /*
    *//*
    public void m7a(Intent intent) {
        String a = C0089a.m408a(intent);
        if (!TextUtils.isEmpty(this.confdb.getUserId()) && !TextUtils.isEmpty(a)) {
            this.apiRequestManager.m144a(a);
        }
    }*/

   /*
    public void m8a(TraceMode.AppState appState) {
        if (appState == null) {
            try {
                BarikoiTraceLogView.onFailure(BarikoiTraceErrors.noDataError());
            } catch (Exception e) {
            }
        } else {
            BarikoiTraceLogView.onSuccess(appState.toString() + " tracking enabled");
            this.confdb.updateAppTrackingState(appState);
            this.locationTracker.m85e();
        }
    }*/

    /*
    *//*
    public void m9a(TraceMode.DesiredAccuracy desiredAccuracy, int i) {
        if (TextUtils.isEmpty(this.confdb.getUserId())) {
            BarikoiTraceLogView.onFailure(BarikoiTraceErrors.noUserError());
        } else if (!SystemSettingsManager.checkKItkatPermission(this.context) || !SystemSettingsManager.checkLocationSettings(this.context)) {
            BarikoiTraceLogView.onFailure(BarikoiTraceErrors.LocationPermissionError());
        } else {
            this.locationTracker.m80a(desiredAccuracy, i, (BarikoiTraceLocationCallback) null);
        }
    }

    *//*
    *//*
    public void m10a(TraceMode.DesiredAccuracy desiredAccuracy, int i, BarikoiTraceLocationCallback traceLocationCallback) {
        if (!SystemSettingsManager.checkKItkatPermission(this.context) || !SystemSettingsManager.checkLocationSettings(this.context)) {
            traceLocationCallback.onFailure(BarikoiTraceErrors.LocationPermissionError());
            return;
        }
        try {
            this.locationTracker.m80a(desiredAccuracy, i, traceLocationCallback);
        } catch (BarikoiTraceException e) {
        }
    }*/




    void m15a(String str) {
        setApiKey(str);
//        syncActiveTrip(new BarikoiTraceTripStateCallback() {
//            @Override
//            public void onSuccess() {
//
//            }
//
//            @Override
//            public void onFailure(BarikoiTraceError barikoiError) {
//
//            }
//        });
    }


    public void startTracking(TraceMode traceTrackingMode) {
        try {
            if (TextUtils.isEmpty(this.confdb.getUserID())) {
                BarikoiTraceLogView.onFailure(BarikoiTraceErrors.noUserError());
            }  else if (!SystemSettingsManager.checkPermissions(this.context) || !SystemSettingsManager.checkLocationSettings(this.context)) {
                BarikoiTraceLogView.onFailure(BarikoiTraceErrors.LocationPermissionError());
            } else {
                BarikoiTraceLogView.onSuccess("Tracking Started " );
                this.confdb.turnTrackingOn();
                this.confdb.setTraceMode(traceTrackingMode);
                this.locationTracker.startLocationService();
            }
        } catch (Exception e) {
        }
    }



    public void setApiKey(String str) {
        Context context = this.context;
        ((Application) context).registerActivityLifecycleCallbacks(new ApplicationBinder(context, this.confdb, this.locationTracker));
        this.confdb.setApiKey(str);
        apiRequestManager.setKey(str);
        setLogging(true);
        if (!NetworkChecker.isNetworkAvailable(this.context)) {
            BarikoiTraceLogView.onFailure(BarikoiTraceErrors.networkError());
        } else if (TextUtils.isEmpty(str)) {
            BarikoiTraceLogView.onFailure(BarikoiTraceErrors.noKeyError());
        }
    }



    public void getUserInfo(String str, BarikoiTraceUserCallback barikoiUserCallback) {
        if (!NetworkChecker.isNetworkAvailable(this.context)) {
            barikoiUserCallback.onFailure(BarikoiTraceErrors.networkError());
        } else if (TextUtils.isEmpty(str)) {
            barikoiUserCallback.onFailure(BarikoiTraceErrors.noDataError());
        } else if (TextUtils.isEmpty(this.confdb.getApiKey())) {
            barikoiUserCallback.onFailure(BarikoiTraceErrors.noKeyError());
        }else{

        }
    }
    void setEmail(String email, BarikoiTraceUserCallback callback){
        if (!NetworkChecker.isNetworkAvailable(this.context)) {
            callback.onFailure(BarikoiTraceErrors.networkError());
        } else if (TextUtils.isEmpty(email)) {
            callback.onFailure(BarikoiTraceErrors.noDataError());
        } else if (TextUtils.isEmpty(this.confdb.getApiKey())) {
            callback.onFailure(BarikoiTraceErrors.noKeyError());
        }else{
            this.apiRequestManager.setUser(email,null,callback);
        }

    }
    void setPhone(String phone, BarikoiTraceUserCallback callback){
        if (!NetworkChecker.isNetworkAvailable(this.context)) {
            callback.onFailure(BarikoiTraceErrors.networkError());
        } else if (TextUtils.isEmpty(phone)) {
            callback.onFailure(BarikoiTraceErrors.noDataError());
        } else if (TextUtils.isEmpty(this.confdb.getApiKey())) {
            callback.onFailure(BarikoiTraceErrors.noKeyError());
        }else {
            this.apiRequestManager.setUser(null, phone, callback);
        }
    }
    void setOrCreateUser(String name, String email, String phone, final BarikoiTraceUserCallback callback){

        if (!NetworkChecker.isNetworkAvailable(this.context)) {
            callback.onFailure(BarikoiTraceErrors.networkError());
        } else if (TextUtils.isEmpty(phone)) {
            callback.onFailure(BarikoiTraceErrors.noDataError());
        } else if (TextUtils.isEmpty(this.confdb.getApiKey())) {
            callback.onFailure(BarikoiTraceErrors.noKeyError());
        } else if(TextUtils.isEmpty(phone)){
            callback.onFailure(new BarikoiTraceError("BK402","user login or register requires phone number"));
        }else {
            this.apiRequestManager.setorCreateUser(name, email, phone, new BarikoiTraceUserCallback() {
                @Override
                public void onFailure(BarikoiTraceError barikoiError) {
                    callback.onFailure(barikoiError);
                }

                @Override
                public void onSuccess(BarikoiTraceUser traceUser) {
                    LogDbHelper.getInstance(context).setUserid(traceUser.getUserId());
                    callback.onSuccess(traceUser);
                }
            });
        }
    }
    void setUserId(String user_id){
        this.confdb.setUserID(user_id);
    }

    String getUserId(){return this.confdb.getUserID();}


    public void syncActiveTrip(final BarikoiTraceTripStateCallback callback) {
        if (!NetworkChecker.isNetworkAvailable(this.context)) {
            callback.onFailure(BarikoiTraceErrors.networkError());
        } else if (TextUtils.isEmpty(this.confdb.getUserID())) {
            callback.onFailure(BarikoiTraceErrors.noUserError());
        } else if (TextUtils.isEmpty(this.confdb.getApiKey())) {
            callback.onFailure(BarikoiTraceErrors.noKeyError());
        }else
        ApiRequestManager.getInstance(context).getCurrentTrips(new BarikoiTraceGetTripCallback() {
            @Override
            public void onSuccess(ArrayList<Trip> trips) {
                if(trips.size()>0){
                    if(!isOnTrip()){
                        confdb.setOnTrip(true);
                        confdb.turnTrackingOn();
                        locationTracker.startLocationService();
                    }
                    if(!locationTracker.isTrackingOn()){
                        locationTracker.startLocationService();
                    }
                }else if(trips.size()==0){
                    if(isOnTrip()){
                        confdb.setOnTrip(false);
                        //confdb.stopSdkTracking();
                        locationTracker.stopLocationService();
                    }

                }
                callback.onSuccess();
            }

            @Override
            public void onFailure(BarikoiTraceError barikoiError) {
                callback.onFailure(barikoiError);
            }
        });


    }

    boolean m32b() {
        return SystemSettingsManager.checkPermissions(this.context) ;
    }






    public void setLogging(boolean z) {
        this.confdb.setLogging(z);
    }



    public boolean checkLocationSettings() {
        return SystemSettingsManager.checkLocationSettings(this.context);
    }


    public void m36d() {
        this.confdb.setAccuracyEngine(false);
        BarikoiTraceLogView.onSuccess("Accuracy engine disabled");
    }

    /*
    *//*
    public void m37d(String str) {
        this.confdb.m242e(str);
        if (!NetworkChecker.isNetworkAvailable(this.context)) {
            BarikoiTraceLogView.onFailure(BarikoiTraceErrors.networkError());
        } else if (TextUtils.isEmpty(str)) {
            BarikoiTraceLogView.onFailure(BarikoiTraceErrors.noDataError());
        } else if (!TextUtils.isEmpty(this.confdb.getUserId())) {
            this.apiRequestManager.m152b();
        }
    }*/





    void requestBatteryOptimization(Context context) {
        SystemSettingsManager.requestBatteryOptimizationSetting(context);
    }





    public void enableAccuracyEngine() {
        this.confdb.setAccuracyEngine(true);
        BarikoiTraceLogView.onSuccess("Accuracy engine enabled");
    }




    public String m47h() {
        return this.confdb.getDeviceToken();
    }





    public boolean checkIgnoringBatteryOptimization() {
        return SystemSettingsManager.isIgnoringBatteryOptimization(this.context);
    }



    public boolean m50j() {
        return this.locationTracker.isTrackingOn();
    }


    public void startGeofence(double lat, double lon, int radius){
        this.locationTracker.startGeofence(lat,lon,radius);
    }


    public void stopTracking() {
        //LogDbHelper.getInstance(this.context).m312a("Tracking stopped");
        BarikoiTraceLogView.onSuccess("Tracking stopped");
        this.confdb.stopSdkTracking();
        this.locationTracker.stopLocationService();
    }

    public void startTrip(String tag, TraceMode traceMode, BarikoiTraceTripStateCallback callback){

        locationTracker.startTrip(tag, traceMode,callback);

    }

    public void stopTrip( BarikoiTraceTripStateCallback callback){
        locationTracker.stopTrip(callback);

    }

    public boolean isOnTrip(){
        return locationTracker.isOnTrip();
    }

    public void uploadOfflineData(){
        locationTracker.uploadOfflineData();
    }

    public void updateCurrentLocation(BarikoiTraceLocationUpdateCallback callback){
        locationTracker.updateCurrentLocation(new LocationUpdateListener() {
            @Override
            public void onLocationReceived(Location location) {
                apiRequestManager.sendLocation(location, new BarikoiTraceLocationUpdateCallback() {
                    @Override
                    public void onlocationUpdate(Location location) {
                        callback.onlocationUpdate(location);
                    }

                    @Override
                    public void onFailure(BarikoiTraceError barikoiError) {
                        try {
                            locationTracker.saveLoctoDb(location);
                        } catch (BarikoiTraceException e) {
                            e.printStackTrace();
                        }
                        callback.onFailure(barikoiError);
                    }
                });
            }

            @Override
            public void onFailure(BarikoiTraceError barikoiError) {
                callback.onFailure(barikoiError);
            }

            @Override
            public void onProviderAvailabilityChanged(boolean available) {

            }
        });
    }

    public void getCompanySettings(BarikoiTraceSettingsCallback callback){
        apiRequestManager.syncSettings(callback);
    }
    public void syncTripstate(BarikoiTraceTripStateCallback callback){
        syncActiveTrip(callback);
    }
    public void setOFflineTracking(boolean enabled) {
        this.confdb.setOfflineTracking(enabled);
    }

    public void setTraceMode(TraceMode mode) {
        confdb.setTraceMode(mode);
    }
}
