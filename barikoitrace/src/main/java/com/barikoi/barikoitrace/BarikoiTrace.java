package com.barikoi.barikoitrace;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.util.List;

public class BarikoiTrace {
    private static final String TAG=BarikoiTrace.class.getName();
    static final String APIKEY_TAG="barikoi_apikey";
    static final String BARIKOI_ID_TAG="barikoi_userid";
    public static final int BARIKOI_REQUEST_CHECK_PERMISSION=1069;
    private static BarikoiTrace INSTANCE;
    private Context context;
    public BarikoiTrace(Context context,String apikey){
        this.context=context;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences.Editor editor=prefs.edit();
        editor.putString(APIKEY_TAG, apikey);
        editor.apply();
    }
    public static BarikoiTrace getInstance(Context context, String apikey){
        if (INSTANCE==null)
            INSTANCE= new BarikoiTrace(context.getApplicationContext(),apikey);
        return INSTANCE;
    }

    public BarikoiTrace setUserId(int id){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences.Editor editor=prefs.edit();
        editor.putInt(BARIKOI_ID_TAG, id);
        editor.apply();
        return this;
    }

    public  void startTracking( ) {
        if (isTrackingOn()) {
            Log.d("BarikoiTrace","already running no need to start again");

            } else {
            if(checkPermissions()) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
                String apikey = prefs.getString(APIKEY_TAG, "");
                int id = prefs.getInt(BARIKOI_ID_TAG, 0);
                if (apikey.equals("")) {
                    Log.e(TAG, "barikoi api key missing");
                } else if (id == 0) {
                    Log.e(TAG, "barikoi user id missing");
                } else {
                    Intent intent = new Intent(context, TimerService.class).putExtra(APIKEY_TAG, apikey).putExtra(BARIKOI_ID_TAG, id);
                    context.startService(intent);
                }
            }
            else{
                requestPermissions();
            }
        }

    }

    public void stopTracking(){
        if (isTrackingOn( )) {
            context.stopService(new Intent(context, TimerService.class));

        }
    }


    public  boolean isTrackingOn() {

        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);

        if (services != null) {
            for (int i = 0; i < services.size(); i++) {
                if ((TimerService.class.getName()).equals(services.get(i).service.getClassName()) && services.get(i).pid != 0) {
                    return true;
                }
            }

        }
        return false;
    }

    public boolean checkPermissions(){
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            return false;
        } else {
            return true;
        }
    }
    public void requestPermissions(){
        ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BARIKOI_REQUEST_CHECK_PERMISSION);
    }
    public void permissionGranted(boolean granted){
        if (granted){
            startTracking();
        }else{
            Log.d(TAG,"permission denied");
        }
    }
}
