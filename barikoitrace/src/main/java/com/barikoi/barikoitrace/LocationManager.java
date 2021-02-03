package com.barikoi.barikoitrace;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;


import com.barikoi.barikoitrace.Utils.NetworkChecker;
import com.barikoi.barikoitrace.Utils.SystemSettingsManager;
import com.barikoi.barikoitrace.callback.BarikoiTraceUserCallback;
import com.barikoi.barikoitrace.exceptions.BarikoiTraceLogView;
import com.barikoi.barikoitrace.localstorage.ConfigStorageManager;
import com.barikoi.barikoitrace.localstorage.sqlitedb.LogDbHelper;
import com.barikoi.barikoitrace.models.BarikoiTraceErrors;
import com.barikoi.barikoitrace.network.ApiRequestManager;
import com.barikoi.barikoitrace.p000b.LocationTracker;
import com.barikoi.barikoitrace.p000b.p001c.ApplicationBinder;

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






    public Bundle m6a(Map<String, String> map) {
        Bundle bundle = new Bundle();
        try {
            if (map.size() > 0) {
                bundle.putString("type", map.get("type"));
                bundle.putString("cid", map.get("cid"));
            }
        } catch (Exception e) {
        }
        return bundle;
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
    }












    public boolean m27a() {
        return SystemSettingsManager.checkBackgroundLocationPermission(this.context);
    }


    public void m28b(TraceMode traceTrackingMode) {
        try {
            if (TextUtils.isEmpty(this.confdb.getUserID())) {
                BarikoiTraceLogView.onFailure(BarikoiTraceErrors.noUserError());
            }  else if (!SystemSettingsManager.checkPermissions(this.context) || !SystemSettingsManager.checkLocationSettings(this.context)) {
                BarikoiTraceLogView.onFailure(BarikoiTraceErrors.LocationPermissionError());
            } else {

                BarikoiTraceLogView.onSuccess("Tracking Started");
                this.confdb.turnTrackingOn(traceTrackingMode);
                this.locationTracker.startLocationService();
            }
        } catch (Exception e) {
        }
    }



    public void setApiKey(String str) {
        Context context = this.context;
        ((Application) context).registerActivityLifecycleCallbacks(new ApplicationBinder(context, this.confdb, this.locationTracker));
        this.confdb.setApiKey(str);
        if (!NetworkChecker.isNetworkAvailable(this.context)) {
            BarikoiTraceLogView.onFailure(BarikoiTraceErrors.networkError());
        } else if (TextUtils.isEmpty(str)) {
            BarikoiTraceLogView.onFailure(BarikoiTraceErrors.noDataError());
        }
    }



    public void getUserInfo(String str, BarikoiTraceUserCallback barikoiUserCallback) {
        if (!NetworkChecker.isNetworkAvailable(this.context)) {
            barikoiUserCallback.onFailure(BarikoiTraceErrors.networkError());
        } else if (TextUtils.isEmpty(str)) {
            barikoiUserCallback.onFailure(BarikoiTraceErrors.noDataError());
        } else if (TextUtils.isEmpty(this.confdb.getApiKey())) {
            barikoiUserCallback.onFailure(BarikoiTraceErrors.noDataError());
        }else{

        }
    }
    void setEmail(String email, BarikoiTraceUserCallback callback){
        if (!NetworkChecker.isNetworkAvailable(this.context)) {
            callback.onFailure(BarikoiTraceErrors.networkError());
        } else if (TextUtils.isEmpty(email)) {
            callback.onFailure(BarikoiTraceErrors.noDataError());
        } else if (TextUtils.isEmpty(this.confdb.getApiKey())) {
            callback.onFailure(BarikoiTraceErrors.noDataError());
        }else{
            this.apiRequestManager.getUser(email,null,callback);
        }

    }
    void setPhone(String phone, BarikoiTraceUserCallback callback){
        if (!NetworkChecker.isNetworkAvailable(this.context)) {
            callback.onFailure(BarikoiTraceErrors.networkError());
        } else if (TextUtils.isEmpty(phone)) {
            callback.onFailure(BarikoiTraceErrors.noDataError());
        } else if (TextUtils.isEmpty(this.confdb.getApiKey())) {
            callback.onFailure(BarikoiTraceErrors.noDataError());
        }else {
            this.apiRequestManager.getUser(null, phone, callback);
        }
    }
    void setUserId(String user_id){
        this.confdb.setUserID(user_id);
    }



    boolean m32b() {
        return SystemSettingsManager.checkPermissions(this.context) ;
    }






    public void m34c(boolean z) {
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





    void requestBatteryOptimization() {
        SystemSettingsManager.requestBatteryOptimizationSetting(this.context);
    }




    public void m42f() {
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
        return this.confdb.isSdkTracking();
    }




    public void stopTracking() {
        //LogDbHelper.getInstance(this.context).m312a("Tracking stopped");
        BarikoiTraceLogView.onSuccess("Tracking stopped");
        this.confdb.stopSdkTracking();
        this.locationTracker.stopLocationService();
    }
}
