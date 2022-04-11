package com.barikoi.barikoitrace;

import android.app.Activity;
import android.content.Context;

import com.barikoi.barikoitrace.Utils.SystemSettingsManager;
import com.barikoi.barikoitrace.callback.BarikoiTraceSettingsCallback;
import com.barikoi.barikoitrace.callback.BarikoiTraceTripStateCallback;
import com.barikoi.barikoitrace.callback.BarikoiTraceUserCallback;
import com.barikoi.barikoitrace.exceptions.BarikoiTraceLogView;
import com.barikoi.barikoitrace.exceptions.ContextException;
import com.barikoi.barikoitrace.models.BarikoiTraceErrors;

public class BarikoiTrace {
    private static final String TAG=BarikoiTrace.class.getName();
    public static final String EXTRA="BarikoiTraceHandler";


    //public static final int REQUEST_CODE_BACKGROUND_LOCATION_PERMISSION = 10222;
    public static final int REQUEST_CODE_LOCATION_ENABLED = 10225;
    public static final int REQUEST_CODE_LOCATION_PERMISSION = 10221;

    private static LocationManager manager;
    private Context context;

    /**
     * Initializes the BarikoiTrace module with API key and context
     *
     * @param context   Application or Activity context
     * @param apikey    API key from Barikoi Trace
     */
    public static void initialize(Context context, String apikey){
        manager = LocationManager.getInstance(context);
        getInstance().m15a(apikey);
    }
    /**
     * set Barikoi User by the user ID
     *
     * @param id ID of the BarikoiTrace user in STRING
     */
    public void setUserId(String id){
        getInstance().setUserId(id);
    }

    public static String getUserId() { return getInstance().getUserId();}
    @Deprecated
    public static void setEmail(String email, BarikoiTraceUserCallback callback){
        getInstance().setEmail(email,callback);
    }
    @Deprecated
    public static void setPhone(String phone, BarikoiTraceUserCallback callback){
        getInstance().setPhone(phone,callback);
    }

    @Deprecated
    public static void setOrCreateUser (String email,String phone, BarikoiTraceUserCallback callback){
        getInstance().setOrCreateUser(null,email,phone,callback);
    }

    /**
     * Logs in an user using name, email, phone number. If user does not exist, create an user and return the user info.
     *
     * @param name
     * @param email
     * @param phone
     * @param callback {@link BarikoiTraceUserCallback}
     */
    public static void setOrCreateUser (String name, String email,String phone, BarikoiTraceUserCallback callback){
        getInstance().setOrCreateUser(name,email,phone,callback);
    }



    /*public static boolean isBackgroundLocationPermissionGranted() {
        return getInstance().m27a();
    }
*/

    /**
     * returns whether location permissions area granted
     *
     * @return {@link Boolean}
     */
    public static boolean isLocationPermissionsGranted() {
        return getInstance().m32b();
    }

    /**
     * Return whether location settings is turned on
     *
     * @return {@link Boolean}
     */
    public static boolean isLocationSettingsOn() {
        return getInstance().checkLocationSettings();
    }

    /**
     * Opents Autostart settings intent for some custom android OS. Autostart settings is needed for loation service management
     * @param context
     */
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


    /**
     * set Barikoi User by the user ID
     *
     * @param user_id ID of the BarikoiTrace user in STRING
     */
    public static void setUser(String user_id){
        getInstance().setUserId(user_id);
    }

    /**
     *
     * @return Whether the Barrety optimization ignore settings is enabled
     */
    public static boolean isBatteryOptimizationEnabled() {
        return getInstance().checkIgnoringBatteryOptimization();
    }

    /**
     *
     * @return whether location service is on
     */
    public static boolean isLocationTracking() {
        return getInstance().m50j();
    }



    public static void requestLocationPermissions(Activity activity){
        SystemSettingsManager.requestLocationPermissions(activity);
    }

    /*public static void requestBackgroundLocationPermission(Activity activity) {
        SystemSettingsManager.requestAndroidPbackgroundLocationPermission(activity);
    }*/


    public static void requestLocationServices(Activity activity) {
        SystemSettingsManager.requestLocationSettings(activity);
    }


    public static void setTraceMode(TraceMode mode){
        getInstance().setTraceMode(mode);
    }

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

    public static void getSettingsfromRemote(BarikoiTraceSettingsCallback callback){
        getInstance().getCompanySettings(callback);
    }
    public static void syncTripstate(BarikoiTraceTripStateCallback callback){
        getInstance().syncTripstate(callback);
    }

    public static void checkAppServicePermission(Context context){
        SystemSettingsManager.checkAppServicePermission(context);
    }

    public static void setLoggingEnabled(boolean enabled){
        getInstance().setLogging(enabled);
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
