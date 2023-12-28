package com.barikoi.barikoitrace.Utils;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.barikoi.barikoitrace.BarikoiTrace;
import com.google.android.gms.common.GoogleApiAvailability;

import java.security.Permission;
import java.util.List;


import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.POST_NOTIFICATIONS;
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


    /*public static boolean checkBackgroundLocationPermission(Context context) {
        return Build.VERSION.SDK_INT >= 29 && ContextCompat.checkSelfPermission(context, ACCESS_BACKGROUND_LOCATION) == 0;
    }*/


    public static boolean checkifMockprovider(Context context, Location location) {
        return Build.VERSION.SDK_INT >= 18 ? location.isFromMockProvider() : !Settings.Secure.getString(context.getContentResolver(), "mock_location").equals("0");
    }


    public static void requestLocationPermissions(Activity activity) {

        ActivityCompat.requestPermissions(activity, new String[]{ACCESS_FINE_LOCATION}, (int) BarikoiTrace.REQUEST_CODE_LOCATION_PERMISSION);
    }

    public static void requestNotificationPermission(Activity activity) {
        if(ActivityCompat.checkSelfPermission(activity, POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT>=33) {
            ActivityCompat.requestPermissions(activity, new String[]{POST_NOTIFICATIONS},  BarikoiTrace.REQUEST_CODE_NOTIFICATION_PERMISSION);
        }
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
            return Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE, 0) != 0;
        }

        String string = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        return string.contains("gps") && string.contains("network");
    }


    public static boolean checkKitktatSorageWritePermission(Context context) {
        return Build.VERSION.SDK_INT >= 19 && ContextCompat.checkSelfPermission(context, WRITE_EXTERNAL_STORAGE) == 0;
    }


    public static void requestBatteryOptimizationSetting(Context context) {
        if (Build.VERSION.SDK_INT >= 23) {
            Intent intent = new Intent();
            String packageName = context.getApplicationContext().getPackageName();
            PowerManager powerManager = (PowerManager) context.getSystemService(context.POWER_SERVICE);
            if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
//                intent.setData(Uri.parse("package:" + packageName));
                context.startActivity(intent);
            }
        }
    }
    public static void openAutostartSettings( Context context) {
        try {
            Intent intent = new Intent();
            String manufacturer = Build.MANUFACTURER;
            if ("xiaomi".equalsIgnoreCase(manufacturer)) {
                intent.setComponent( new ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                ));
            } else if ("oppo".equalsIgnoreCase(manufacturer)) {
                intent.setComponent( new ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                )); //need "oppo.permission.OPPO_COMPONENT_SAFE" in the manifest
            } else if ("vivo".equalsIgnoreCase(manufacturer)) {
                intent.setComponent( new ComponentName(
                        "com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                ));
            } else if ("Letv".equalsIgnoreCase(manufacturer)) {
                intent.setComponent( new ComponentName(
                        "com.letv.android.letvsafe",
                        "com.letv.android.letvsafe.AutobootManageActivity"
                ));
            } else if ("Honor".equalsIgnoreCase(manufacturer)) {
                intent.setComponent( new ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.optimize.process.ProtectActivity"
                ));
            } else {
                //Timber.d("Auto-start permission not necessary")
            }
            Log.d("autostartpermission", intent.getComponent().getPackageName());
            List list = context.getPackageManager()
                    .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if (list.size() > 0) {
                context.startActivity(intent);
            }
        } catch (Exception e) {
        }
    }

    public static void checkAppServicePermission(Context context){
        Intent[] POWERMANAGER_INTENTS = {
                new Intent().setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
                new Intent().setComponent(new ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity")),
                new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")),
                new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
                new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")),
                new Intent().setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
                new Intent().setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")),
                new Intent().setComponent(new ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")),
                new Intent().setComponent(new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
                new Intent().setComponent(new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")),
                new Intent().setComponent(new ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
                new Intent().setComponent(new ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")),
                new Intent().setComponent(new ComponentName("com.htc.pitroad", "com.htc.pitroad.landingpage.activity.LandingPageActivity")),
                new Intent().setComponent(new ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.MainActivity"))
        };



        for (Intent intent : POWERMANAGER_INTENTS)
            if (context.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                // show dialog to ask user action
                //Log.d("packagecheck", intent.getComponent().toString());
                context.startActivity(intent);
                break;
            }

    }

    public static boolean isInPowerSaveMode(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return ((PowerManager) context.getSystemService(context.POWER_SERVICE)).isPowerSaveMode();
        }
        return false;
    }


    public static boolean isinDozeMode(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return Build.VERSION.SDK_INT>= Build.VERSION_CODES.M && pm.isDeviceIdleMode();
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
