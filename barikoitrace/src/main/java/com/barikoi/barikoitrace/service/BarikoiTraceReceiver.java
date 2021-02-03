package com.barikoi.barikoitrace.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import androidx.annotation.Keep;

import com.barikoi.barikoitrace.Utils.DateTimeUtils;
import com.barikoi.barikoitrace.exceptions.BarikoiTraceLogView;
import com.barikoi.barikoitrace.localstorage.ConfigStorageManager;
import com.barikoi.barikoitrace.models.BarikoiTraceError;
import com.barikoi.barikoitrace.models.BarikoiTraceLocation;
import com.barikoi.barikoitrace.models.BarikoiTraceLocationInfo;
import com.barikoi.barikoitrace.models.events.BarikoiTraceEvent;
import com.barikoi.barikoitrace.models.events.BarikoiTraceEvents;
import com.google.gson.Gson;

@Keep
public class BarikoiTraceReceiver extends BroadcastReceiver {
    public void onError(Context context, BarikoiTraceError barikoiError) {
    }

    public void onEventReceived(Context context, BarikoiTraceEvent traceEvent) {

    }

    public void onLocationReceived(Context context, BarikoiTraceLocationInfo barikoiLocationInfo) {
        BarikoiTraceLogView.debugLog(barikoiLocationInfo.getCoordinates().getCoordinates().toString());
    }

    public void onLocationUpdated(Context context, BarikoiTraceLocation traceLocation) {
        BarikoiTraceLogView.debugLog(traceLocation.getLocation().toString());
    }

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            try {
                if (intent.getAction().equals("com.barikoi.trace.android.RECEIVED") && context.getPackageName().equals(intent.getExtras().getString("packageName"))) {
                    ConfigStorageManager a = ConfigStorageManager.getInstance(context);
                    BarikoiTraceError barikoiError = (BarikoiTraceError) intent.getExtras().getSerializable("error");
                    Location location = (Location) intent.getExtras().getParcelable("location");
                    String string = intent.getExtras().getString("activity");
                    BarikoiTraceLogView.debugLog(intent.getExtras().toString());
                    if (!(location == null || string == null )) {
                        onLocationUpdated(context, new BarikoiTraceLocation(a.getUserID(), location, string, DateTimeUtils.getCurrentDateTimeUTC(), DateTimeUtils.getCurrentDateTimeStringPST()));
                    }
                    Gson gson = new Gson();
                    BarikoiTraceEvents traceEvents = (BarikoiTraceEvents) gson.fromJson(intent.getExtras().getString("com.barikoi.trace.android.EVENTS", null), BarikoiTraceEvents.class);
                    if (traceEvents != null && traceEvents.getEvents().size() > 0) {
                        onEventReceived(context, traceEvents.getEvents().get(0));
                    }
                    BarikoiTraceLocationInfo barikoiLocationInfo = (BarikoiTraceLocationInfo) gson.fromJson(intent.getExtras().getString("com.barikoi.trace.android.USER.LOCATION", null), BarikoiTraceLocationInfo.class);
                    if (barikoiLocationInfo != null) {
                        onLocationReceived(context, barikoiLocationInfo);
                    }

                    if (barikoiError != null) {
                        onError(context, barikoiError);
                    }
                }
            } catch (Exception e) {
            }
        }
    }


}
