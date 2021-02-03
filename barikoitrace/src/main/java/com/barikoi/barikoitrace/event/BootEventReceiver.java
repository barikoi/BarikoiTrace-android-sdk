package com.barikoi.barikoitrace.event;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;

import com.barikoi.barikoitrace.Utils.SystemSettingsManager;
import com.barikoi.barikoitrace.exceptions.BarikoiTraceLogView;
import com.barikoi.barikoitrace.localstorage.ConfigStorageManager;
import com.barikoi.barikoitrace.p000b.LocationTracker;


public class BootEventReceiver extends BroadcastReceiver {
    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        try {
            BarikoiTraceLogView.debugLog("received msg"+intent.getData().toString());
            LocationTracker bVar = new LocationTracker(context);
            String action = intent.getAction();
            char c = 65535;
            int hashCode = action.hashCode();
            if (hashCode != -1184851779) {
                if (hashCode != -1085845802) {
                    if (hashCode == 798292259 && action.equals(Intent.ACTION_BOOT_COMPLETED)) {
                        c = 1;
                    }
                }
            } else if (action.equals(LocationManager.PROVIDERS_CHANGED_ACTION)) {
                c = 0;
            }
            if (c == 0) {
                bVar.m74a();

            } else if (c == 1) {
                bVar.m85e();
            }
        } catch (Exception e) {
        }
    }
}
