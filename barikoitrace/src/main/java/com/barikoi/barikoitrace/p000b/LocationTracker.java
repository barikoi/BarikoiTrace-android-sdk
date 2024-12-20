package com.barikoi.barikoitrace.p000b;

import static android.os.Build.*;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.barikoi.barikoitrace.TraceMode;
import com.barikoi.barikoitrace.utils.DateTimeUtils;
import com.barikoi.barikoitrace.utils.NetworkChecker;
import com.barikoi.barikoitrace.utils.SystemSettingsManager;
import com.barikoi.barikoitrace.callback.BarikoiTraceBulkUpdateCallback;
import com.barikoi.barikoitrace.callback.BarikoiTraceLocationCallback;
import com.barikoi.barikoitrace.callback.BarikoiTraceLocationUpdateCallback;
import com.barikoi.barikoitrace.callback.BarikoiTraceTripApiCallback;
import com.barikoi.barikoitrace.callback.BarikoiTraceTripStateCallback;
import com.barikoi.barikoitrace.event.BootEventReceiver;
import com.barikoi.barikoitrace.exceptions.BarikoiTraceException;
import com.barikoi.barikoitrace.exceptions.BarikoiTraceLogView;
import com.barikoi.barikoitrace.localstorage.ConfigStorageManager;
import com.barikoi.barikoitrace.localstorage.sqlitedb.LocationDbHelper;
//import com.barikoi.barikoitrace.localstorage.sqlitedb.LogDbHelper;
import com.barikoi.barikoitrace.models.BarikoiTraceError;
import com.barikoi.barikoitrace.models.BarikoiTraceErrors;
import com.barikoi.barikoitrace.models.LocationUtils;
import com.barikoi.barikoitrace.models.createtrip.Trip;
import com.barikoi.barikoitrace.network.ApiRequestManager;
import com.barikoi.barikoitrace.network.JsonResponseAdapter;
import com.barikoi.barikoitrace.p000b.p001c.DeviceInfo;
import com.barikoi.barikoitrace.p000b.p002d.LocationUpdateListener;
import com.barikoi.barikoitrace.p000b.p002d.UnifiedLocationManager;
import com.barikoi.barikoitrace.service.BarikoiTraceLocationService;
import com.barikoi.barikoitrace.service.LocationWork;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;


public final class LocationTracker implements LocationUpdateListener {


    private static BootEventReceiver bootEventReceiver;


    private Context context;


    private ConfigStorageManager storageManager;


    private LocationDbHelper locdbhelper;


//    private LogDbHelper logdb;


    private BarikoiTraceLocationCallback locationCallback;




    public LocationTracker(Context context) {
        this.context = context;
//        this.logdb = LogDbHelper.getInstance(context);
        this.locdbhelper = LocationDbHelper.getInstance(context);
        this.storageManager = ConfigStorageManager.getInstance(context);
    }


    private void m72b(Location location) throws BarikoiTraceException {
        try {
            sendLocationBroadCast(location, LocationUtils.LocationStatus.STOP.toString(), (BarikoiTraceError) null);

            DeviceInfo.updateBatteryInfo(this.context);
            boolean a = NetworkChecker.isNetworkAvailable(this.context);

            if (a) {

            }
        } catch (Exception e) {
            throw new BarikoiTraceException(e);
        }
    }


    private void m73m() {


            DeviceInfo.updateBatteryInfo(this.context);
            boolean a = NetworkChecker.isNetworkAvailable(this.context);
            if (a) {

            }

    }

    public void m74a() {
        if (!TextUtils.isEmpty(this.storageManager.getUserID())) {
            if (SystemSettingsManager.checkLocationSettings(this.context)) {
//                this.logdb.writeLog("GPS: enabled");
                this.storageManager.m252h(true);
            } else if (!this.storageManager.getGPSCheck()) {
//                this.logdb.writeLog("GPS: disabled");
                this.storageManager.m252h(false);
                m73m();
            }
        }
    }


    @Override // com.barikoi.barikoitrace.p000b.p002d.LocationUpdateListener

    public void onLocationReceived(Location location) {

        try {
            //logdb.m312a("location update: "+location.getLatitude()+", "+location.getLongitude()+ " time: "+DateTimeUtils.getCurrentTimeLocal());
            if (this.locationCallback != null) {
                this.locationCallback.location(location);
            } else {
                m72b(location);
            }
        } catch (BarikoiTraceException e) {
        }
    }



    public void m77a(final Location location, LocationUtils.LocationStatus bVar) throws BarikoiTraceException {
        try {
            this.storageManager.updateLastLocation(location);
            sendLocationBroadCast(location, bVar.toString(), (BarikoiTraceError) null);
            //DeviceInfo.updateBatteryInfo(this.context);
            boolean a2 = NetworkChecker.isNetworkAvailable(this.context);
            if (a2) {
                if(storageManager.isOfflineTracking()) {
                    if (!storageManager.isDataSyncing() && locdbhelper.getofflinecount() > 0) {
                        uploadOfflineData();
                    }
                }

                ApiRequestManager.getInstance(this.context).sendLocation(location, new BarikoiTraceLocationUpdateCallback() {
                    @Override
                    public void onlocationUpdate(Location location) {
                        Log.d("trace", "Location Updated");
                    }

                    @Override
                    public void onFailure(BarikoiTraceError barikoiError) {
                        BarikoiTraceLogView.onFailure(barikoiError);
                        if(barikoiError.equals(BarikoiTraceErrors.networkError()) && storageManager.isOfflineTracking()){
                            try {
                                Log.d("trace", "Location sned failed, saved offline");
                                locdbhelper.insertLocation(JsonResponseAdapter.getlocationJson(location));
                            } catch (BarikoiTraceException e) {
                                Log.e("trace", e.getMessage());
                            }
                        }
                    }
                });
            }else if(storageManager.isOfflineTracking()){
                    locdbhelper.insertLocation(JsonResponseAdapter.getlocationJson(location));
            }
        } catch (Exception e) {
            throw new BarikoiTraceException(e);
        }
    }


    public void uploadOfflineData(){
        storageManager.setDataSyncing(true);
//        logdb.writeLog("location syncing started. offline count: "+locdbhelper.getofflinecount() );
            final JSONArray data=locdbhelper.getLocationJson(storageManager.getUserID());

            ApiRequestManager.getInstance(context).sendOfflineData(data, new BarikoiTraceBulkUpdateCallback() {
                @Override
                public void onBulkUpdate() {
                    if(locdbhelper.clearlast100()>0)
                        uploadOfflineData();
                    else storageManager.setDataSyncing(false);
                }

                @Override
                public void onFailure(BarikoiTraceError barikoiError) {
                    BarikoiTraceLogView.debugLog(data.toString());
                    BarikoiTraceLogView.onFailure(barikoiError);
                    storageManager.setDataSyncing(false);
                }
            });

    }

    public void sendLocationBroadCast(Location location, String str, BarikoiTraceError barikoiError) throws BarikoiTraceException {
        try {
            Intent intent = new Intent();
            intent.setAction("com.barikoi.trace.android.RECEIVED");
            intent.putExtra("packageName", this.context.getPackageName());
            if (barikoiError != null) {
                intent.putExtra("error", barikoiError);
            } else if (!(location == null || str == null)) {
                intent.putExtra("location", location);
                intent.putExtra("activity", str);
            }
            this.context.sendBroadcast(intent);
        } catch (Exception e) {
            throw new BarikoiTraceException(e);
        }
    }



    /*
    public void m80a(TraceMode.DesiredAccuracy desiredAccuracy, int i, BarikoiTraceLocationCallback traceLocationCallback) throws BarikoiTraceException {
        try {
            this.locationCallback = traceLocationCallback;
            new C0022a(this.context, this, i).m117a(desiredAccuracy);
        } catch (Exception e) {
            throw new BarikoiTraceException(e);
        }
    }
*/
    @Override // com.barikoi.barikoitrace.p000b.p002d.LocationUpdateListener

    public void onFailure(BarikoiTraceError barikoiError) {
        try {
            if (this.locationCallback != null) {
                this.locationCallback.onFailure(barikoiError);
            } else {
                sendLocationBroadCast((Location) null, (String) null, barikoiError);
            }
        } catch (BarikoiTraceException e) {
        }
    }

    @Override
    public void onProviderAvailabilityChanged(boolean available) {

    }



   /* public void m83c() {
        this.logdb.m312a("Network: " + NetworkChecker.isNetworkAvailable(this.context));
        if (!TextUtils.isEmpty(this.storageManager.getUserID())) {
            if (NetworkChecker.isNetworkAvailable(this.context) || !this.storageManager.isOfflineTracking()) {


                m85e();
                return;
            }
            stopLocationService();
        }
    }*/


    public void registerReceiver() throws BarikoiTraceException {
        try {
            if (bootEventReceiver == null) {
                bootEventReceiver = new BootEventReceiver();
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
                this.context.registerReceiver(bootEventReceiver, intentFilter);
            }
        } catch (Exception e) {
            throw new BarikoiTraceException(e);
        }
    }


    public void m85e() {

            if (!isTrackingOn() ) {
                return;
            }
            if (!LocationUtils.m410a(this.storageManager.getAppstate(), this.storageManager) || (!NetworkChecker.isNetworkAvailable(this.context) && !this.storageManager.isOfflineTracking())) {
                stopLocationService();
            } else {
                startLocationService();
            }

    }

    private void periodicLocationUpdate() {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.
                Builder(LocationWork.class,15, TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(context).
                enqueueUniquePeriodicWork("UploadLocation",
                        ExistingPeriodicWorkPolicy.REPLACE,workRequest);
    }

    private void stopPeriodicLocationUpdate() {
        WorkManager.getInstance(context).cancelUniqueWork("UploadLocation");
    }

    public void startLocationService() {
          periodicLocationUpdate();
//        if (!isTrackingOn() ) {
//            logdb.writeLog("location service starting");
            if (VERSION.SDK_INT >= VERSION_CODES.O) {
                this.context.startForegroundService(new Intent(this.context, BarikoiTraceLocationService.class));
            }else{
                this.context.startService(new Intent(this.context, BarikoiTraceLocationService.class));
            }
            periodicLocationUpdate();
//        }else logdb.writeLog("location service already running ");
    }

    public boolean isTrackingOn() {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (BarikoiTraceLocationService.class.getName().equals(service.service.getClassName()) && service.started) {
                Log.d("locationservice",service.process+ " "+service.foreground+ " "+ new Date(service.activeSince).toString());
                return true;
            }
        }
        return false;
    }

    public void stopLocationService() {
//            logdb.writeLog("location service stopping");
//            logdb.generateDBFile();
            this.context.stopService(new Intent(this.context, BarikoiTraceLocationService.class));
            stopPeriodicLocationUpdate();
            this.storageManager.m235c();

    }

    public void updateCurrentLocation(LocationUpdateListener singlelocUpdateListener){
        new UnifiedLocationManager(context, singlelocUpdateListener).oneTimeLocationUpdate();
    }

    public void saveLoctoDb(Location location) throws BarikoiTraceException {
        locdbhelper.insertLocation(JsonResponseAdapter.getlocationJson(location));
    }




    public void startTrip(final String tag, final TraceMode traceMode, final BarikoiTraceTripStateCallback callback){
        final String startTime= DateTimeUtils.getCurrentTimeLocal();
//        logdb.writeLog("requested start trip");
//        logdb.writeDeviceLog();
        storageManager.setTraceMode(traceMode);
        ApiRequestManager.getInstance(context).startTrip(startTime, traceMode, tag, new BarikoiTraceTripApiCallback() {
            @Override
            public void onFailure(BarikoiTraceError barikoiError) {
//                logdb.writeLog("start trip failed: "+barikoiError.getMessage());
                //locdbhelper.addTrip(Integer.parseInt(storageManager.getUserID()),startTime,tag, 0);
                callback.onFailure(barikoiError);
            }

            @Override
            public void onSuccess(Trip trip) {
//                logdb.writeLog("trip started succesfully ");

                storageManager.setOnTrip(true);
                storageManager.turnTrackingOn();
                //storageManager.turnTrackingOn(traceMode);
                startLocationService();
                callback.onSuccess(trip);
                //locdbhelper.addTrip(Integer.parseInt(storageManager.getUserID()),startTime,tag, 1);
            }
        });

    }

    public void stopTrip(final BarikoiTraceTripStateCallback callback){

        final String endTime= DateTimeUtils.getCurrentTimeLocal();

        if(isOnTrip()) {
            if(locdbhelper.getofflinecount()>0 && !storageManager.isDataSyncing()){
                uploadOfflineData();
            }
            ApiRequestManager.getInstance(context).endTrip(endTime, new BarikoiTraceTripApiCallback() {
                @Override
                public void onFailure(BarikoiTraceError barikoiError) {
                    callback.onFailure(barikoiError);
//                    logdb.writeLog("trip end failed: "+barikoiError.getMessage());
                    //locdbhelper.endTrip(Integer.parseInt(storageManager.getUserID()), endTime, 2);
                }

                @Override
                public void onSuccess(Trip trip) {
                    storageManager.setOnTrip(false);
//                    logdb.writeLog("Trip ended successfully ");
//                    logdb.generateDBFile();
                    storageManager.stopSdkTracking();
                    stopLocationService();
                    callback.onSuccess(trip);
                    //locdbhelper.endTrip(Integer.parseInt(storageManager.getUserID()), endTime, 1);
                }
            });
            //if (locdbhelper.getActiveTrips().get(0).getSynced() == 1)
            /*else {
                locdbhelper.endTrip(Integer.parseInt(storageManager.getUserID()), endTime, 0);
                syncOfflineTrips();
            }*/
        }else {
//            logdb.writeLog("Trip end failed, "+BarikoiTraceErrors.tripStateError().getMessage());
            storageManager.stopSdkTracking();
            stopLocationService();
            callback.onFailure(BarikoiTraceErrors.tripStateError());
        }
    }



    public boolean isOnTrip(){
        return storageManager.isOnTrip();
    }

    public void syncOfflineTrips(){
        ArrayList<Trip> trips=locdbhelper.getofflineTrips();
        for(final Trip trip: trips){
            if (trip.getSynced()==0)
            ApiRequestManager.getInstance(context).syncOfflineTrip(trip, new BarikoiTraceTripApiCallback() {
                @Override
                public void onFailure(BarikoiTraceError barikoiError) {

                }

                @Override
                public void onSuccess(Trip trip) {
                    locdbhelper.removeTrip(trip.getTrip_id());
                }
            });
            else if(trip.getSynced()==2){
                ApiRequestManager.getInstance(context).endTrip(trip.getEnd_time(), new BarikoiTraceTripApiCallback() {
                    @Override
                    public void onFailure(BarikoiTraceError barikoiError) {

                    }

                    @Override
                    public void onSuccess(Trip trip) {
                        locdbhelper.removeTrip(trip.getTrip_id());
                    }
                });
            }
        }
    }

    public void startGeofence(double lat, double lon, int radius){
        this.context.startService(new Intent(this.context, BarikoiTraceLocationService.class)
                .putExtra("type","geofence")
                .putExtra("latitude",lat)
        .putExtra("longitude",lon)
        .putExtra("radius",radius));
    }


    public void unregisterReceiver() throws BarikoiTraceException {
        try {
            if (bootEventReceiver != null) {
                this.context.unregisterReceiver(bootEventReceiver);
                bootEventReceiver = null;
            }
        } catch (Exception e) {
            throw new BarikoiTraceException(e);
        }
    }

}
