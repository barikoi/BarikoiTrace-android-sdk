package com.barikoi.barikoitrace.p000b.p001c;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import com.barikoi.barikoitrace.localstorage.ConfigStorageManager;


public final class DeviceInfo {

    public static int getBattery(Intent intent) {
        int i = 0;
        boolean booleanExtra = intent.getBooleanExtra("present", false);
        int intExtra = intent.getIntExtra("scale", -1);
        int intExtra2 = intent.getIntExtra("level", -1);
        if (!booleanExtra) {
            return 5;
        }
        if (intExtra2 >= 0 && intExtra > 0) {
            i = (intExtra2 * 100) / intExtra;
        }
        return Math.round((float) i);
    }


    public static String getBrand() {
        return Build.BRAND;
    }


    public static void updateBatteryInfo(Context context) {
        try {
            Intent registerReceiver = context.registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
            ConfigStorageManager a = ConfigStorageManager.getInstance(context);
            a.setBatteryState(getBatteryStatus(registerReceiver));
            a.updateBattery((long) getBattery(registerReceiver));
        } catch (Exception e) {
        }
    }


    public static String getModel() {
        return Build.MODEL;
    }


    public static String getBatteryStatus(Intent intent) {
        int intExtra = intent.getIntExtra("status", -1);
        return intExtra != 2 ? intExtra != 4 ? intExtra != 5 ? "unknown" : "full" : "unplugged" : "charging";
    }


    public static String getBuildRelease() {
        return Build.VERSION.RELEASE;
    }


    public static String m99d() {
        return "3.1.0";
    }


    public static String getBuildDisplay() {
        return Build.DISPLAY;
    }


    public static String getSdkVersion() {
        return String.valueOf(Build.VERSION.SDK_INT);
    }
}
