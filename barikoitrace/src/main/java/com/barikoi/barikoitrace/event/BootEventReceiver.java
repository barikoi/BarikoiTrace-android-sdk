package com.barikoi.barikoitrace.event;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.util.Log;
import android.widget.Toast;

import com.barikoi.barikoitrace.p000b.GeofenceManager;
import com.barikoi.barikoitrace.p000b.LocationTracker;


public class BootEventReceiver extends BroadcastReceiver {
    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        try {
            //BarikoiTraceLogView.debugLog("received msg"+intent.getData().toString());
            LocationTracker bVar = new LocationTracker(context);
            String action = intent.getAction();
            char c = 65535;
            int hashCode = action.hashCode();

            if (hashCode !=  -1184851779) {
                if (hashCode != -1085845802) {
                    if (hashCode == 798292259 && action.equals(Intent.ACTION_BOOT_COMPLETED)) {
                        c = 1;
                    }
                }
            }if (action.equals("barikoitrace.barikoitracereceiver.GEOFENCE")) {
                Toast.makeText(context, "geofence event received", Toast.LENGTH_SHORT).show();
                c = 2;
            } else if (action.equals(LocationManager.PROVIDERS_CHANGED_ACTION)) {
                c = 0;
            }
            if (c == 0) {

                bVar.m74a();

            } else if (c == 1) {
                bVar.m85e();
            } else if (c == 2) {
                GeofenceManager.getInstance(context).onGeofenceEventReceived(intent);
            }
        } catch (Exception e) {
            Log.d("BarikoiTraceException", e.getMessage());
        }
    }
}
