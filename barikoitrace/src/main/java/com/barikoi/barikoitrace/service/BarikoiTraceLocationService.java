package com.barikoi.barikoitrace.service;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;


import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.barikoi.barikoitrace.BarikoiTrace;
import com.barikoi.barikoitrace.R;
import com.barikoi.barikoitrace.TraceMode;
import com.barikoi.barikoitrace.models.BarikoiTraceUser;
import com.barikoi.barikoitrace.network.Api;
import com.barikoi.barikoitrace.network.MQTTClientManager;
import com.barikoi.barikoitrace.utils.DateTimeUtils;
import com.barikoi.barikoitrace.exceptions.BarikoiTraceException;
import com.barikoi.barikoitrace.exceptions.BarikoiTraceLogView;
import com.barikoi.barikoitrace.localstorage.ConfigStorageManager;
import com.barikoi.barikoitrace.models.BarikoiTraceError;
import com.barikoi.barikoitrace.models.LocationUtils;
import com.barikoi.barikoitrace.p000b.LocationTracker;
import com.barikoi.barikoitrace.p000b.p002d.LocationUpdateListener;
import com.barikoi.barikoitrace.p000b.p002d.UnifiedLocationManager;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static androidx.core.app.NotificationCompat.PRIORITY_MAX;


public class BarikoiTraceLocationService extends Service implements LocationUpdateListener {


    private ConfigStorageManager configStorageManager;

    private LocationTracker locationTracker;

    private UnifiedLocationManager unifiedLocationManager;


    //private LogDbHelper logDbHelper;
    private BroadcastReceiver powerSaveModeReceiver;
    private MQTTClientManager mqttManager;
    private List<Integer> f252e = new ArrayList<>();


    private int activeDistFilter = 0;


    private int f254g = 0;
    private PowerManager.WakeLock wakeLock;

//    private SocketManager socketManager;

    private void startLocationUpdate() {
        TraceMode mode=this.configStorageManager.getTraceMode();
        if (mode.getUpdateInterval() > 0) {
            UnifiedLocationManager cVar = this.unifiedLocationManager;
            cVar.removeLocationUpdate();
            cVar.startLocationUpdate(configStorageManager, mode.getUpdateInterval(),0,mode.getPingSyncInterval());
            return;

        }else if(mode.getDistanceFilter()>0){
            this.unifiedLocationManager.removeLocationUpdate();
            this.unifiedLocationManager.startLocationUpdate(configStorageManager, mode.getUpdateInterval(), mode.getDistanceFilter(), mode.getPingSyncInterval());
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
        if( configStorageManager.getTraceMode().getEndTime()!= LocalTime.MAX ){
            if(LocalTime.now().isAfter(configStorageManager.getTraceMode().getEndTime()) || LocalTime.now().isBefore(configStorageManager.getTraceMode().getStartTime())){
                configStorageManager.stopSdkTracking();
//                mqttManager.destroy();
                stopSelf();
                return;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if(location.isMock()){
                Toast.makeText(this, "Mock location detected", Toast.LENGTH_SHORT).show();
            }
        }else if (location.isFromMockProvider())
            Toast.makeText(this, "Mock location detected", Toast.LENGTH_SHORT).show();

        if (isValid(location)) {
            if(configStorageManager.isbroadcastingEnabled()) {
                broadcastLocation(location);
            }
            BarikoiTraceLogView.debugLog("location : accuracy "+location.getAccuracy() + ", time: "+ DateTimeUtils.getDateTimeLocal(location.getTime()));
            this.f254g = 0;
            //this.logDbHelper.m312a("Location " + location.getLatitude() + "--" + location.getLongitude() + "--" + this.activeDistFilter + "--" + a);
            if (this.configStorageManager.getType() == TraceMode.TrackingModes.CUSTOM.getOption()) {
                BarikoiTraceLogView.debugLog("custom "+ configStorageManager.getUpdateInterval());
//                try {
//                    this.locationTracker.m77a(location, LocationUtils.LocationStatus.MOVING);
//                } catch (BarikoiTraceException e) {
//                    throw new RuntimeException(e);
//                }
//                socketManager.sendLocation(location);
                mqttManager.publishLocation(location);
                return;
            }
//            int speed = (int) LocationUtils.getSpeedInKmph(location.getSpeed());
//            BarikoiTraceLogView.debugLog(activeDistFilter+"");
//                m523a(location, speed);
            return;
        }
        this.f254g++;
            /*this.unifiedLocationManager.removeLocationUpdate();
            startLocationUpdate();*/
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
        startForegroundNotification();

        // Register power save mode receiver
//        registerPowerSaveReceiver();
    }

    /**
     * Ensure the service is running in foreground mode
     * Call this at the start of onStartCommand to prevent ForegroundServiceDidNotStartInTimeException
     */
    private void ensureForegroundStarted() {
        // This ensures foreground is started even if onCreate hasn't been called yet
        // or if the service is being restarted
        try {
            startForegroundNotification();
        } catch (Exception e) {
            Log.e("BarikoiTrace", "Error ensuring foreground started", e);
        }
    }

    /**
     * Start the foreground notification
     */
    private void startForegroundNotification() {
        String CHANNEL_ID = "BarikoiTrace";
        String CHANNEL_NAME = "Trace is running as Background service";

        NotificationChannel channel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel(CHANNEL_ID,
                    CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setContentText(CHANNEL_NAME)
                    .setSmallIcon(R.drawable.ic_trace_logo)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager. TYPE_NOTIFICATION ))
                    .setOngoing(true)
                    .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                    .build();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1, notification, FOREGROUND_SERVICE_TYPE_LOCATION);
            }else startForeground(1, notification);
        }else {

            Notification notification2 = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setContentText(CHANNEL_NAME)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager. TYPE_NOTIFICATION ))
                    .setSmallIcon(R.drawable.ic_trace_logo)
                    .setOngoing(true)
                    .build();
            startForeground(1, notification2);
        }
    }

    @Override // android.app.Service
    public void onDestroy() {
        if(wakeLock!=null) wakeLock.release();
//        socketManager.disconnect();
        if (mqttManager != null) {
            mqttManager.destroy();

        }

        // Release wake lock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }

        // Unregister receivers
        if (powerSaveModeReceiver != null) {
            unregisterReceiver(powerSaveModeReceiver);
            powerSaveModeReceiver = null;
        }
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

        // Ensure foreground notification is active before any initialization
        // This prevents ForegroundServiceDidNotStartInTimeException
        ensureForegroundStarted();

        try {

            this.configStorageManager = ConfigStorageManager.getInstance(this);
            this.locationTracker = new LocationTracker(this);
            BarikoiTraceUser user = configStorageManager.getUser() ;
            String uuid = configStorageManager.getDeviceToken();

            String mqtt_url = configStorageManager.getMqttUrl()==null? Api.mqtt_url:configStorageManager.getMqttUrl();
            // Initialize MQTT client manager
            initializeMqttManager(mqtt_url,user.getUserId(), uuid, user.getCompanyId(), user.getGroup());

            this.unifiedLocationManager = new UnifiedLocationManager(this, this);
//            socketManager= SocketManager.getInstance(configStorageManager.getApiKey(),configStorageManager.getUserID());
//            socketManager.connect();
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

    /**
     * Initialize MQTT manager with callbacks
     */
    private void initializeMqttManager( String mqtt_url,String id,String uuid, String company, String group) {
         mqttManager = new MQTTClientManager(
                this,
                mqtt_url,// Replace with your broker address
                id,
                company,
                group,
                uuid,
                new MQTTClientManager.MqttStatusCallback() {
                    @Override
                    public void onConnectionStatusChanged(boolean connected, String message) {
                        // Update notification with connection status
//                        updateNotification(connected ?
//                                "Location service connected" :
//                                "Location service: " + message);
                    }

                    @Override
                    public void onMessageDelivered(String topic) {
                        // Handle message delivery confirmation if needed
                    }

                    @Override
                    public void onMessageReceived(String topic, String message) {
                        // Process incoming messages if needed
                        if (topic.endsWith("/command")) {
                            handleCommand(message);
                        }
                    }
                });

        // Connect to MQTT broker
        mqttManager.connect();
    }
    /**
     * Update the service notification
     */
    private void updateNotification(String text) {
        NotificationManager notificationManager = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager = getSystemService(NotificationManager.class);
        }
        if (notificationManager != null) {
            notificationManager.notify(1, createNotification(text));
        }
    }

    private Notification createNotification(String text) {
        String CHANNEL_ID = "BarikoiTrace";
        String CHANNEL_NAME = "Trace is running as Background service";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            return new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setContentText(text)
                    .setSmallIcon(R.drawable.ic_trace_logo)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager. TYPE_NOTIFICATION ))
                    .setOngoing(true)
                    .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                    .build();
        }else {

            return new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setContentText(text)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager. TYPE_NOTIFICATION ))
                    .setSmallIcon(R.drawable.ic_trace_logo)
                    .setOngoing(true)
                    .build();
        }
    }

    /**
     * Handle commands received via MQTT
     */
    private void handleCommand(String commandJson) {
        // Process commands received from server
        // e.g., change location reporting frequency
        Log.d("TraceLocationService", "Received command: " + commandJson);
    }



    private void broadcastLocation(Location location) {
        Intent intent = new Intent("com.barikoi.trace.android.RECEIVED");
        intent.putExtra("location", location);
        intent.putExtra("event", "LOCATION_RECEIEVED");

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Register broadcast receiver for power save mode changes
     */
    private void registerPowerSaveReceiver() {
        powerSaveModeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                if (pm != null) {
                    boolean isPowerSaveMode = pm.isPowerSaveMode();
                    Log.d("TraceLocationService", "Power save mode changed: " + isPowerSaveMode);

                    // Update MQTT parameters based on power mode
                    if (mqttManager != null) {
                        mqttManager.handlePowerSaveMode(isPowerSaveMode);
                    }
                }
            }
        };

        registerReceiver(powerSaveModeReceiver,
                new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED));
    }



}
