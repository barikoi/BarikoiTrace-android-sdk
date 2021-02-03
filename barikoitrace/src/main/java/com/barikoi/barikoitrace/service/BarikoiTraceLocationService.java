package com.barikoi.barikoitrace.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;


import androidx.core.app.NotificationCompat;

import com.barikoi.barikoitrace.R;
import com.barikoi.barikoitrace.TraceMode;
import com.barikoi.barikoitrace.exceptions.BarikoiTraceException;
import com.barikoi.barikoitrace.exceptions.BarikoiTraceLogView;
import com.barikoi.barikoitrace.localstorage.ConfigStorageManager;
import com.barikoi.barikoitrace.localstorage.sqlitedb.LogDbHelper;
import com.barikoi.barikoitrace.models.BarikoiTraceError;
import com.barikoi.barikoitrace.models.C0089a;
import com.barikoi.barikoitrace.network.ApiRequestManager;
import com.barikoi.barikoitrace.p000b.LocationTracker;
import com.barikoi.barikoitrace.p000b.p002d.LocationUpdateListener;
import com.barikoi.barikoitrace.p000b.p002d.UnifiedLocationManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static androidx.core.app.NotificationCompat.PRIORITY_MAX;

public class BarikoiTraceLocationService extends Service implements LocationUpdateListener {


    private ConfigStorageManager configStorageManager;


    private LocationTracker locationTracker;


    private UnifiedLocationManager unifiedLocationManager;


    private LogDbHelper logDbHelper;


    private List<Integer> f252e = new ArrayList();


    private int f253f = 0;


    private int f254g = 0;
    private ApiRequestManager apiRequestManager;


    private void m522a() {
        if (this.configStorageManager.getUpdateInterval() > 0) {
            UnifiedLocationManager cVar = this.unifiedLocationManager;
            ConfigStorageManager aVar = this.configStorageManager;
            cVar.startLocationUpdate(aVar, aVar.getUpdateInterval(), this.f253f);
            return;

        }
        int a = C0089a.m403a(this.configStorageManager, 0);
        this.f253f = a;
        this.unifiedLocationManager.startLocationUpdate(this.configStorageManager, 0, a);
    }


    private void m523a(Location location, int i) throws BarikoiTraceException {
        try {
            int a = C0089a.m404a(this.configStorageManager, this.f252e, location, i);
            if (this.f253f < a || this.f253f > a) {
                if (this.unifiedLocationManager != null) {
                    this.unifiedLocationManager.removeLocationUpdate();
                }
                this.f253f = a;
                this.logDbHelper.m312a("Distance filter updated:  " + this.f253f);
                this.unifiedLocationManager.startLocationUpdate(this.configStorageManager, 0, this.f253f);
            }
            this.locationTracker.m77a(location, C0089a.EnumC0091b.MOVING);
        } catch (Exception e) {
            throw new BarikoiTraceException(e);
        }
    }


    private boolean m524b(Location location) {
        boolean z = true;
        if (!this.configStorageManager.getAccuracyEngine() || this.f254g >= 1) {
            return true;
        }
        if (new Date().getTime() - location.getTime() > 10000 || location.getAccuracy() < 0.0f) {
            return false;
        }
        if (location.getAccuracy() > ((float) C0089a.m402a(this.configStorageManager))) {
            z = false;
        }
        return z;
    }

    @Override // com.barikoi.barikoitrace.p000b.p002d.LocationUpdateListener

    public void onLocationReceived(Location location) {
        try {
            if (m524b(location)) {
                BarikoiTraceLogView.debugLog("valid location found");

                int a = (int) C0089a.m400a(location.getSpeed());
                this.f254g = 0;
                this.logDbHelper.m312a("Location " + location.getLatitude() + "--" + location.getLongitude() + "--" + this.f253f + "--" + a);
                if (this.configStorageManager.getType() == TraceMode.TrackingModes.CUSTOM.getOption()) {
                    
                    this.locationTracker.m77a(location, C0089a.EnumC0091b.MOVING);
                    return;
                }

                m523a(location, a);
                return;
            }
            this.f254g++;
            this.unifiedLocationManager.removeLocationUpdate();
            m522a();
        } catch (BarikoiTraceException e) {
        }
    }

    @Override // com.barikoi.barikoitrace.p000b.p002d.LocationUpdateListener

    public void onFailure(BarikoiTraceError barikoiError) {
        try {
            BarikoiTraceLogView.onFailure(barikoiError);
            this.locationTracker.sendLocationBroadCast((Location) null, (String) null, barikoiError);
        } catch (BarikoiTraceException e) {
        }
    }

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override // android.app.Service
    public void onCreate() {
        super.onCreate();
        String CHANNEL_ID = "BarikoiTrace ";
        String CHANNEL_NAME = "BarikoiTrace is running as Background service";

        NotificationChannel channel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel(CHANNEL_ID,
                    CHANNEL_NAME, NotificationManager.IMPORTANCE_NONE);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setContentText(CHANNEL_NAME)
                    .setSmallIcon(R.drawable.ic_trace_logo)
                    .setPriority(PRIORITY_MAX)
                    .build();
            startForeground(1, notification);
        }else {
//            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this,CHANNEL_ID)
//                    .setSmallIcon(R.drawable.barikoi_logo)
//                    .setContentTitle(CHANNEL_ID)
//                    .setContentText(CHANNEL_NAME)
//                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            Notification notification2 = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setContentText(CHANNEL_NAME)
                    .setSmallIcon(R.drawable.ic_trace_logo)
                    .setPriority(PRIORITY_MAX)
                    .build();

            //NotificationManager notifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            //notifyMgr.notify(101, notification2);
            //managerCompat.notify(1,mBuilder.build());
            startForeground(1, notification2);
        }
        try {
            LogDbHelper a = LogDbHelper.getInstance(this);
            this.logDbHelper = a;
            //a.m312a("BarikoiTraceLocationService:  onCreate");
            this.configStorageManager = ConfigStorageManager.getInstance(this);
            this.locationTracker = new LocationTracker(this);
            this.unifiedLocationManager = new UnifiedLocationManager(this, this);
            this.apiRequestManager= ApiRequestManager.getInstance(this);
            m522a();
        } catch (Exception e) {
        }
    }

    @Override // android.app.Service
    public void onDestroy() {
        super.onDestroy();
        try {
            this.logDbHelper.m312a("BarikoiTraceLocationService:  onDestroy");
            if (this.unifiedLocationManager != null) {
                this.unifiedLocationManager.removeLocationUpdate();
                this.unifiedLocationManager = null;
            }
        } catch (Exception e) {
        }
    }



    @Override // android.app.Service
    public int onStartCommand(Intent intent, int i, int i2) {

        BarikoiTraceLogView.onSuccess("service started");
        return Service.START_STICKY;
    }
}
