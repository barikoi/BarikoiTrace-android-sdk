package com.barikoi.barikoitrace;

import android.app.Activity;
import android.content.Context;

import com.barikoi.barikoitrace.Utils.SystemSettingsManager;
import com.barikoi.barikoitrace.callback.BarikoiTraceTripStateCallback;
import com.barikoi.barikoitrace.callback.BarikoiTraceUserCallback;
import com.barikoi.barikoitrace.exceptions.BarikoiTraceLogView;
import com.barikoi.barikoitrace.exceptions.ContextException;
import com.barikoi.barikoitrace.models.BarikoiTraceErrors;

public class BarikoiTrace {
    private static final String TAG=BarikoiTrace.class.getName();
    public static final String EXTRA="BarikoiTraceHandler";


    public static final int REQUEST_CODE_BACKGROUND_LOCATION_PERMISSION = 10222;
    public static final int REQUEST_CODE_LOCATION_ENABLED = 10225;
    public static final int REQUEST_CODE_LOCATION_PERMISSION = 10221;

    private static LocationManager manager;
    private Context context;


    public static void initialize(Context context, String apikey){
        manager = LocationManager.getInstance(context);
        getInstance().m15a(apikey);

    }
    public void setUserId(String id){
        getInstance().setUserId(id);

    }
    public static void setEmail(String email, BarikoiTraceUserCallback callback){
        getInstance().setEmail(email,callback);
    }
    public static void setPhone(String phone, BarikoiTraceUserCallback callback){
        getInstance().setPhone(phone,callback);
    }




    public static boolean isBackgroundLocationPermissionGranted() {
        return getInstance().m27a();
    }

    public static boolean isLocationPermissionsGranted() {
        return getInstance().m32b();
    }

    public static boolean isLocationSettingsOn() {
        return getInstance().checkLocationSettings();
    }


    public static void openAutostartsettings(Context context){
        SystemSettingsManager.openAutostartSettings(context);
    }




    public static void disableBatteryOptimization() {
        getInstance().requestBatteryOptimization();
    }


   /* public static void getCurrentLocation(TraceMode.DesiredAccuracy desiredAccuracy, int i, BarikoiTraceLocationCallback traceLocationCallback) {
        if (i > 10) {
            getInstance().m10a(desiredAccuracy, i, traceLocationCallback);
        } else {
            getInstance().m10a(desiredAccuracy, 10, traceLocationCallback);
        }
    }
*/
    /*public static String getDeviceToken() {
        return getInstance().m47h();
    }
*/


    private static LocationManager getInstance() {
        LocationManager aVar = manager;
        if (aVar != null) {
            return aVar;
        }
        throw new ContextException("BarikoiTrace instance can't be with null context");
    }



    public static void setUser(String user_id){
        getInstance().setUserId(user_id);
    }



    public static boolean isBatteryOptimizationEnabled() {
        return getInstance().checkIgnoringBatteryOptimization();
    }

    public static boolean isLocationTracking() {
        return getInstance().m50j();
    }



    public static void requestLocationPermissions(Activity activity){
        SystemSettingsManager.requestLocationPermissions(activity);
    }

    public static void requestBackgroundLocationPermission(Activity activity) {
        SystemSettingsManager.requestAndroidPbackgroundLocationPermission(activity);
    }


    public static void requestLocationServices(Activity activity) {
        SystemSettingsManager.requestLocationSettings(activity);
    }



    /*public static void startGeofence(double lat,double lon , int radius){
        getInstance().startGeofence(lat,lon, radius);
    }*/

    public static void startTracking(TraceMode traceTrackingMode) {
        if (traceTrackingMode == null) {
            BarikoiTraceLogView.onFailure(BarikoiTraceErrors.noDataError());
        } else {
            getInstance().startTracking(traceTrackingMode);
        }
    }

    public static void startTrip(String tag, TraceMode traceTrackingMode, BarikoiTraceTripStateCallback callback){
        getInstance().startTrip(tag,traceTrackingMode, callback);
    }

    public static void endTrip(BarikoiTraceTripStateCallback callback){
        getInstance().stopTrip(callback);
    }
    public static  boolean isOnTrip(){
        return getInstance().isOnTrip();
    }

    public static void stopTracking() {
        getInstance().stopTracking();
    }


    public static void setOfflineTracking(boolean enabled){
        getInstance().setOFflineTracking(enabled);
    }

    public static void syncTripstate(BarikoiTraceTripStateCallback callback){
        getInstance().syncTripstate(callback);
    }

/*
    public static void updateCurrentLocation(TraceMode.DesiredAccuracy desiredAccuracy, int i) {
        if (i > 10) {
            getInstance().m9a(desiredAccuracy, i);
        } else {
            getInstance().m9a(desiredAccuracy, 10);
        }
    }*/
}
