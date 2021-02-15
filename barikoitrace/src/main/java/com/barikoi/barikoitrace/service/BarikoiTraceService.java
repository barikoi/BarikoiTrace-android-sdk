package com.barikoi.barikoitrace.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.barikoi.barikoitrace.Utils.NetworkChangeManager;
import com.barikoi.barikoitrace.exceptions.BarikoiTraceException;
import com.barikoi.barikoitrace.exceptions.BarikoiTraceLogView;
import com.barikoi.barikoitrace.p000b.LocationTracker;


public class BarikoiTraceService extends Service {


    private LocationTracker locationTracker;


    private NetworkChangeManager networkChangeManager;

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override // android.app.Service
    public void onCreate() {
        super.onCreate();
        try {
            //BarikoiTraceLogView.debugLog("BarikoiTraceService started");
            this.networkChangeManager = new NetworkChangeManager(this);
            LocationTracker locationTracker = new LocationTracker(this);
            this.locationTracker = locationTracker;
            locationTracker.registerReceiver();
            this.networkChangeManager.registerReceiver();
        } catch (BarikoiTraceException e) {
        }
    }

    @Override // android.app.Service
    public void onDestroy() {
        //BarikoiTraceLogView.debugLog("BarikoiTraceService destroyed");
        try {
            if (this.networkChangeManager != null) {
                this.networkChangeManager.unregisterReceiver();
            }
            if (this.locationTracker != null) {
                this.locationTracker.unregisterReceiver();
            }
        } catch (BarikoiTraceException e) {
        }
        super.onDestroy();
    }

    @Override // android.app.Service
    public int onStartCommand(Intent intent, int i, int i2) {
        return START_STICKY;

    }
}
