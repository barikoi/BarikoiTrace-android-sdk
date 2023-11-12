package com.barikoi.barikoitrace.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;


import androidx.core.app.NotificationCompat;

import com.barikoi.barikoitrace.R;
import com.barikoi.barikoitrace.TraceMode;
import com.barikoi.barikoitrace.Utils.DateTimeUtils;
import com.barikoi.barikoitrace.exceptions.BarikoiTraceException;
import com.barikoi.barikoitrace.exceptions.BarikoiTraceLogView;
import com.barikoi.barikoitrace.localstorage.ConfigStorageManager;
import com.barikoi.barikoitrace.models.BarikoiTraceError;
import com.barikoi.barikoitrace.models.LocationUtils;
import com.barikoi.barikoitrace.p000b.LocationTracker;
import com.barikoi.barikoitrace.p000b.p002d.LocationUpdateListener;
import com.barikoi.barikoitrace.p000b.p002d.UnifiedLocationManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static androidx.core.app.NotificationCompat.PRIORITY_LOW;
import static androidx.core.app.NotificationCompat.PRIORITY_MAX;
import static androidx.core.app.NotificationCompat.PRIORITY_MIN;

public class BarikoiTraceLocationService extends Service implements LocationUpdateListener {


    private ConfigStorageManager configStorageManager;

    private LocationTracker locationTracker;

    private UnifiedLocationManager unifiedLocationManager;


    //private LogDbHelper logDbHelper;


    private List<Integer> f252e = new ArrayList();


    private int activeDistFilter = 0;


    private int f254g = 0;
    private PowerManager.WakeLock wakeLock;


    private void startLocationUpdate() {
        TraceMode mode=this.configStorageManager.getTraceMode();
        if (mode.getUpdateInterval() > 0) {
            UnifiedLocationManager cVar = this.unifiedLocationManager;
            cVar.removeLocationUpdate();
            cVar.startLocationUpdate(configStorageManager, mode.getUpdateInterval(), this.activeDistFilter,mode.getPingSyncInterval());
            return;

        }else if(mode.getDistanceFilter()>0){
            this.unifiedLocationManager.removeLocationUpdate();
            this.unifiedLocationManager.startLocationUpdate(configStorageManager, mode.getUpdateInterval(), this.activeDistFilter, mode.getPingSyncInterval());
            return;
        }
        int a = LocationUtils.getDistFilterFromSpeed(this.configStorageManager, 0);
        this.activeDistFilter = a;
        this.unifiedLocationManager.startLocationUpdate(this.configStorageManager, 0, a, mode.getPingSyncInterval());
    }


    private void m523a(Location location, int speed) throws BarikoiTraceException {
        try {
            int a = LocationUtils.m404a(this.configStorageManager, this.f252e, location, speed);
            if (this.activeDistFilter < a || this.activeDistFilter > a) {
                if (this.unifiedLocationManager != null) {
                    this.unifiedLocationManager.removeLocationUpdate();
                }
                this.activeDistFilter = a;
                //this.logDbHelper.m312a("Distance filter updated:  " + this.activeDistFilter);
                this.unifiedLocationManager.startLocationUpdate(this.configStorageManager, 0, this.activeDistFilter, this.configStorageManager.getTraceMode().getPingSyncInterval());
            }
            this.locationTracker.m77a(location, LocationUtils.LocationStatus.MOVING);
        } catch (Exception e) {
            throw new BarikoiTraceException(e);
        }
    }


    private boolean isValid(Location location) {
        boolean z = true;

        if (new Date().getTime() - location.getTime() > 10000 || location.getAccuracy() < 0.0f) {
            return false;
        }
        if (location.getAccuracy() > ((float) LocationUtils.getAccuracyRounded(this.configStorageManager))) {
            z = false;
        }
        return z;
    }

    @Override // com.barikoi.barikoitrace.p000b.p002d.LocationUpdateListener

    public void onLocationReceived(Location location) {
        try {
            if (isValid(location)) {
                BarikoiTraceLogView.debugLog("location : accuracy "+location.getAccuracy() + ", time: "+ DateTimeUtils.getDateTimeLocal(location.getTime()));
                this.f254g = 0;
                //this.logDbHelper.m312a("Location " + location.getLatitude() + "--" + location.getLongitude() + "--" + this.activeDistFilter + "--" + a);
                if (this.configStorageManager.getType() == TraceMode.TrackingModes.CUSTOM.getOption()) {
                    BarikoiTraceLogView.debugLog("custom "+ configStorageManager.getUpdateInterval());
                    this.locationTracker.m77a(location, LocationUtils.LocationStatus.MOVING);
                    return;
                }
                int speed = (int) LocationUtils.getSpeedInKmph(location.getSpeed());
                BarikoiTraceLogView.debugLog(activeDistFilter+"");
                m523a(location, speed);
                return;
            }
            this.f254g++;
            /*this.unifiedLocationManager.removeLocationUpdate();
            startLocationUpdate();*/
        } catch (BarikoiTraceException e) {
        }
    }

    @Override

    public void onFailure(BarikoiTraceError barikoiError) {
        BarikoiTraceLogView.onFailure(barikoiError);
        //this.locationTracker.sendLocationBroadCast((Location) null, (String) null, barikoiError);
    }

    @Override
    public void onProviderAvailabilityChanged(boolean available) {
        String CHANNEL_ID = "BarikoiTrace";
        String CHANNEL_NAME = "Need to turn on location service";
        if(!available){
            PackageManager pm = getPackageManager();
            Intent intent=pm.getLaunchIntentForPackage(getApplicationContext().getPackageName());
            PendingIntent contentIntent = PendingIntent.getActivity(this,
                    0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            NotificationChannel channel = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                channel = new NotificationChannel(CHANNEL_ID,
                        CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
                ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
            }
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setCategory(Notification.CATEGORY_ERROR)
                    .setContentText(CHANNEL_NAME)
                    .setSmallIcon(R.drawable.ic_trace_logo)
                    .setPriority(PRIORITY_MAX)
                    .setContentIntent(contentIntent)
                    .build();


            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(2,notification);


        }else{
            NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(2);
        }
    }

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override // android.app.Service
    public void onCreate() {
        super.onCreate();
        String CHANNEL_ID = "BarikoiTrace";
        String CHANNEL_NAME = "BarikoiTrace is running as Background service";

        NotificationChannel channel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel(CHANNEL_ID,
                    CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setContentText(CHANNEL_NAME)
                    .setSmallIcon(R.drawable.ic_trace_logo)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager. TYPE_NOTIFICATION ))
                    .setPriority(PRIORITY_LOW)
                    .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                    .build();
            startForeground(1, notification);
        }else {

            Notification notification2 = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setContentText(CHANNEL_NAME)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager. TYPE_NOTIFICATION ))
                    .setSmallIcon(R.drawable.ic_trace_logo)
                    .setPriority(PRIORITY_MIN)
                    .build();
            startForeground(1, notification2);
        }
    }

    @Override // android.app.Service
    public void onDestroy() {
        if(wakeLock!=null) wakeLock.release();
        configStorageManager.setDataSyncing(false);
        Log.d("killservice", "why did you kill the app man?");
        try {
            //this.logDbHelper.m312a("BarikoiTraceLocationService:  onDestroy");
            if (this.unifiedLocationManager != null) {
                this.unifiedLocationManager.removeLocationUpdate();
                this.unifiedLocationManager = null;
            }
        } catch (Exception e) {
        }
        super.onDestroy();
    }


    @Override // android.app.Service
    public int onStartCommand(Intent intent, int i, int i2) {

        try {

            this.configStorageManager = ConfigStorageManager.getInstance(this);
            this.locationTracker = new LocationTracker(this);
            this.unifiedLocationManager = new UnifiedLocationManager(this, this);
            startLocationUpdate();
        } catch (Exception e) {
        }
        BarikoiTraceLogView.onSuccess("service started");
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        String tag = "BarikoiTrace::LocationManagerService";


        /*if (intent.getStringExtra("type")!=null){
            try {
                double lat =intent.getDoubleExtra("latitude",23.870769);
                double lon = intent.getDoubleExtra("longitude",90.387815);
                int radius= intent.getIntExtra("radius",30);
                GeofenceManager.getInstance(this).createGeofence(lat,lon,radius,10,"testgeofence");
            } catch (BarikoiTraceException e) {
                e.printStackTrace();
            }
        }*/

        return Service.START_STICKY;
    }


}
