package com.barikoi.barikoitrace.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.barikoi.barikoitrace.BarikoiTrace;
import com.google.android.gms.common.GoogleApiAvailability;

import java.security.Permission;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;


public class SystemSettingsManager {

    public static void requestAndroidPbackgroundLocationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= 29) {
            ActivityCompat.requestPermissions(activity, new String[]{ACCESS_BACKGROUND_LOCATION}, (int) BarikoiTrace.REQUEST_CODE_BACKGROUND_LOCATION_PERMISSION);
        }
    }


    public static boolean checkOreo() {
        return Build.VERSION.SDK_INT >= 26;
    }


    public static boolean checkBackgroundLocationPermission(Context context) {
        return Build.VERSION.SDK_INT >= 29 && ContextCompat.checkSelfPermission(context, ACCESS_BACKGROUND_LOCATION) == 0;
    }


    public static boolean checkifMockprovider(Context context, Location location) {
        return Build.VERSION.SDK_INT >= 18 ? location.isFromMockProvider() : !Settings.Secure.getString(context.getContentResolver(), "mock_location").equals("0");
    }


    public static void requestLocationPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{ACCESS_FINE_LOCATION}, (int) BarikoiTrace.REQUEST_CODE_LOCATION_PERMISSION);
    }


    public static boolean checkPermissions(Context context) {
        return ( ContextCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) == 0) /*&& (Build.VERSION.SDK_INT <29||ContextCompat.checkSelfPermission(context, ACCESS_BACKGROUND_LOCATION) == 0)*/;
    }


    public static void requestLocationSettings(Activity activity) {
        activity.startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), BarikoiTrace.REQUEST_CODE_LOCATION_ENABLED);
    }


    public static boolean checkLocationSettings(Context context) {
        int i = Build.VERSION.SDK_INT;
        if (i >= 28) {
            return ((LocationManager) context.getSystemService(context.LOCATION_SERVICE)).isLocationEnabled();
        }
        if (i >= 19) {
            return Settings.Secure.getInt(context.getContentResolver(), "location_mode", 0) != 0;
        }
        String string = Settings.Secure.getString(context.getContentResolver(), "location_providers_allowed");
        return string.contains("gps") && string.contains("network");
    }


    public static boolean checkKitktatSorageWritePermission(Context context) {
        return Build.VERSION.SDK_INT >= 19 && ContextCompat.checkSelfPermission(context, WRITE_EXTERNAL_STORAGE) == 0;
    }


    public static void requestBatteryOptimizationSetting(Context context) {
        if (Build.VERSION.SDK_INT >= 23) {
            Intent intent = new Intent();
            String packageName = context.getPackageName();
            PowerManager powerManager = (PowerManager) context.getSystemService(context.POWER_SERVICE);
            if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                intent.setFlags(intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        }
    }


    public static boolean isPowerSaveMode(Context context) {
        if (Build.VERSION.SDK_INT >= 21) {
            return ((PowerManager) context.getSystemService(context.POWER_SERVICE)).isPowerSaveMode();
        }
        return false;
    }


    public static boolean isAirplaneModeOn(Context context) {
        return Build.VERSION.SDK_INT < 17 || Settings.Global.getInt(context.getContentResolver(), "airplane_mode_on", 0) != 0;
    }


    public static boolean isIgnoringBatteryOptimization(Context context) {
        String packageName = context.getPackageName();
        PowerManager powerManager = (PowerManager) context.getSystemService(context.POWER_SERVICE);
        return Build.VERSION.SDK_INT >= 23 && powerManager != null && powerManager.isIgnoringBatteryOptimizations(packageName);
    }


    public static boolean isGoogleAvailable(Context context) {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == 0;
    }
}
