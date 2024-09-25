package com.barikoi.barikoitrace;

import android.app.Activity;
import android.content.Context;

import com.barikoi.barikoitrace.Utils.SystemSettingsManager;
import com.barikoi.barikoitrace.callback.BarikoiTraceLocationUpdateCallback;
import com.barikoi.barikoitrace.callback.BarikoiTraceSettingsCallback;
import com.barikoi.barikoitrace.callback.BarikoiTraceTripStateCallback;
import com.barikoi.barikoitrace.callback.BarikoiTraceUserCallback;
import com.barikoi.barikoitrace.exceptions.BarikoiTraceLogView;
import com.barikoi.barikoitrace.exceptions.ContextException;
import com.barikoi.barikoitrace.models.BarikoiTraceErrors;
import com.barikoi.barikoitrace.models.BarikoiTraceUser;

/**
 * Barikoi trace base class. entry point for all trace related functions.
 */
public class BarikoiTrace {
    /**
     * The constant EXTRA.
     */
    public static final String EXTRA="BarikoiTraceHandler";


    /**
     * The constant REQUEST_CODE_BACKGROUND_LOCATION_PERMISSION.
     */
    public static final int REQUEST_CODE_BACKGROUND_LOCATION_PERMISSION = 10222;
    /**
     * The constant REQUEST_CODE_LOCATION_ENABLED.
     */
    public static final int REQUEST_CODE_LOCATION_ENABLED = 10225;
    /**
     * The constant REQUEST_CODE_LOCATION_PERMISSION.
     */
    public static final int REQUEST_CODE_LOCATION_PERMISSION = 10221;

    public static final int REQUEST_CODE_NOTIFICATION_PERMISSION = 10226;

    private static LocationManager manager;


    /**
     * Initializes the BarikoiTrace module with API key and context
     *
     * @param context Application or Activity context
     * @param apikey  API key from Barikoi Trace
     */
    public static void initialize(Context context, String apikey){
        manager = LocationManager.getInstance(context);
        getInstance().m15a(apikey);
    }


    /**
     * Gets user id.
     *
     * @return the user id
     */
    public static String getUserId() { return getInstance().getUserId();}


    public static BarikoiTraceUser getUser(){
        return getInstance().getUser();
    }

    /**
     * Set email.
     *
     * @param email    the email
     * @param callback the callback
     */
    @Deprecated
    public static void setEmail(String email, BarikoiTraceUserCallback callback){
        getInstance().setEmail(email,callback);
    }

    /**
     * Set phone.
     *
     * @param phone    the phone
     * @param callback the callback
     */
    @Deprecated
    public static void setPhone(String phone, BarikoiTraceUserCallback callback){
        getInstance().setPhone(phone,callback);
    }

    /**
     * Set or create user.
     *
     * @param email    the email
     * @param phone    the phone
     * @param callback the callback
     */
    @Deprecated
    public static void setOrCreateUser (String email,String phone, BarikoiTraceUserCallback callback){
        getInstance().setOrCreateUser(null,email,phone,callback);
    }

    /**
     * Logs in an user using name, email, phone number. If user does not exist, create an user and return the user info.
     *
     * @param name     the name
     * @param email    the email
     * @param phone    the phone
     * @param callback callback interface for success or failure {@link BarikoiTraceUserCallback}
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
     * Opens Autostart settings intent for some custom android OS. Autostart settings is needed for loation service management
     *
     * @param context the context
     */
    public static void openAutostartsettings(Context context){
        SystemSettingsManager.openAutostartSettings(context);
    }


    /**
     * Request disable battery optimization.
     *
     * @param context the context
     */
    public static void requestDisableBatteryOptimization(Context context) {
        getInstance().requestBatteryOptimization(context);
    }

    /**
     * Disable battery optimization.
     *
     * @param activity the activity
     */
    public static void disableBatteryOptimization(Activity activity) {
        SystemSettingsManager.requestBatteryOptimizationSetting(activity);
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


//    /**
//     * set Barikoi User by the user ID
//     *
//     * @param user_id ID of the BarikoiTrace user in STRING
//     */
//    public static void setUser(String user_id){
//        getInstance().setUserId(user_id);
//    }

    /**
     * Is battery optimization enabled boolean.
     *
     * @return Whether the Barrety optimization ignore settings is enabled
     */
    public static boolean isBatteryOptimizationEnabled() {
        return getInstance().checkIgnoringBatteryOptimization();
    }

    /**
     * Is location tracking boolean.
     *
     * @return whether location tracking service is running
     */
    public static boolean isLocationTracking() {
        return getInstance().m50j();
    }


    /**
     * Request location permissions.
     *
     * @param activity the activity to request location permission from
     */
    public static void requestLocationPermissions(Activity activity){
        SystemSettingsManager.requestLocationPermissions(activity);
    }


    public static void requestNotificationPermission( Activity activity){
        SystemSettingsManager.requestNotificationPermission(activity);
    }

    /**
     * Request background location permission.
     *
     * @param activity the activity to request background location permission from
     */
    public static void requestBackgroundLocationPermission(Activity activity) {
        SystemSettingsManager.requestAndroidPbackgroundLocationPermission(activity);
    }


    /**
     * Opens location settings option to turn the location sttrings on.
     *
     * @param activity the activity to request location settings from
     */
    public static void requestLocationServices(Activity activity) {
        SystemSettingsManager.requestLocationSettings(activity);
    }


    /**
     * Set trace mode.
     *
     * @param mode the mode
     */
    public static void setTraceMode(TraceMode mode){
        getInstance().setTraceMode(mode);
    }

    /**
     * Start tracking.
     *
     * @param traceTrackingMode the trace tracking mode
     */
    public static void startTracking(TraceMode traceTrackingMode) {
        if (traceTrackingMode == null) {
            BarikoiTraceLogView.onFailure(BarikoiTraceErrors.noDataError());
        } else {
            getInstance().startTracking(traceTrackingMode);
        }
    }

    /**
     * Start trip.
     *
     * @param tag               the tag
     * @param traceTrackingMode the trace tracking mode
     * @param callback          the callback
     */
    public static void startTrip(String tag, TraceMode traceTrackingMode, BarikoiTraceTripStateCallback callback){
        getInstance().startTrip(tag,traceTrackingMode, callback);
    }

    /**
     * End trip.
     *
     * @param callback the callback
     */
    public static void endTrip(BarikoiTraceTripStateCallback callback){
        getInstance().stopTrip(callback);
    }

    /**
     * Is on trip boolean.
     *
     * @return the boolean
     */
    public static  boolean isOnTrip(){
        return getInstance().isOnTrip();
    }

    /**
     * Stop tracking.
     */
    public static void stopTracking() {
        getInstance().stopTracking();
    }

    /**
     * Set offline tracking.
     *
     * @param enabled the enabled
     */
    public static void setOfflineTracking(boolean enabled){
        getInstance().setOFflineTracking(enabled);
    }

    /**
     * Get settingsfrom remote.
     *
     * @param callback the callback
     */
    public static void getSettingsfromRemote(BarikoiTraceSettingsCallback callback){
        getInstance().getCompanySettings(callback);
    }

    /**
     * Sync tripstate.
     *
     * @param callback the callback
     */
    public static void syncTripstate(BarikoiTraceTripStateCallback callback){
        getInstance().syncTripstate(callback);
    }

    /**
     * Update current location.
     *
     * @param callback the callback
     */
    public static void updateCurrentLocation(BarikoiTraceLocationUpdateCallback callback){
        manager.updateCurrentLocation(callback);
    }

    /**
     * Upload offline data.
     */
    public static void uploadOfflineData(){
        manager.uploadOfflineData();
    }

    /**
     * Check app service permission.
     *
     * @param context the context
     */
    public static void checkAppServicePermission(Context context){
        SystemSettingsManager.checkAppServicePermission(context);
    }

    /**
     * Set logging enabled.
     *
     * @param enabled the enabled
     */
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
