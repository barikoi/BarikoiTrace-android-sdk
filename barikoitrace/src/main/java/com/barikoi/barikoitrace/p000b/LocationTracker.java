package com.barikoi.barikoitrace.p000b;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;
import android.text.TextUtils;


import com.barikoi.barikoitrace.Utils.NetworkChecker;
import com.barikoi.barikoitrace.Utils.SystemSettingsManager;
import com.barikoi.barikoitrace.callback.BarikoiTraceLocationCallback;
import com.barikoi.barikoitrace.callback.BarikoiTraceLocationUpdateCallback;
import com.barikoi.barikoitrace.event.BootEventReceiver;
import com.barikoi.barikoitrace.exceptions.BarikoiTraceException;
import com.barikoi.barikoitrace.exceptions.BarikoiTraceLogView;
import com.barikoi.barikoitrace.localstorage.ConfigStorageManager;
import com.barikoi.barikoitrace.localstorage.sqlitedb.LocationDbHelper;
import com.barikoi.barikoitrace.localstorage.sqlitedb.LogDbHelper;
import com.barikoi.barikoitrace.models.BarikoiTraceError;
import com.barikoi.barikoitrace.models.C0089a;
import com.barikoi.barikoitrace.network.ApiRequestManager;
import com.barikoi.barikoitrace.p000b.p001c.DeviceInfo;
import com.barikoi.barikoitrace.p000b.p002d.LocationUpdateListener;
import com.barikoi.barikoitrace.service.BarikoiTraceLocationService;


public final class LocationTracker implements LocationUpdateListener {


    private static BootEventReceiver bootEventReceiver;


    private Context context;


    private ConfigStorageManager storageManager;


    private LocationDbHelper locdbhelper;


    private LogDbHelper logdb;


    private BarikoiTraceLocationCallback locationCallback;



    public LocationTracker(Context context) {
        this.context = context;
        this.logdb = LogDbHelper.getInstance(context);
        this.locdbhelper = LocationDbHelper.getInstance(context);
        this.storageManager = ConfigStorageManager.getInstance(context);
    }


    private void m72b(Location location) throws BarikoiTraceException {
        try {
            sendLocationBroadCast(location, C0089a.EnumC0091b.STOP.toString(), (BarikoiTraceError) null);

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
            String a2 = C0089a.m406a();
            if (a) {

            }

    }


    public void m74a() {
        if (!TextUtils.isEmpty(this.storageManager.getUserID())) {
            if (SystemSettingsManager.checkLocationSettings(this.context)) {
                //this.logdb.m312a("GPS: enabled");
                this.storageManager.m252h(false);
            } else if (!this.storageManager.getGPSCheck()) {
                //this.logdb.m312a("GPS: disabled");
                this.storageManager.m252h(true);
                this.storageManager.m235c();
                m73m();
            }
        }
    }


    @Override // com.barikoi.barikoitrace.p000b.p002d.LocationUpdateListener

    public void onLocationReceived(Location location) {
        try {
            if (this.locationCallback != null) {
                this.locationCallback.location(location);
            } else {
                m72b(location);
            }
        } catch (BarikoiTraceException e) {
        }
    }


    public void m77a(Location location, C0089a.EnumC0091b bVar) throws BarikoiTraceException {
        try {
            this.storageManager.updateLastLocation(location);
            sendLocationBroadCast(location, bVar.toString(), (BarikoiTraceError) null);


            DeviceInfo.updateBatteryInfo(this.context);
            boolean a2 = NetworkChecker.isNetworkAvailable(this.context);
            if (a2) {
                ApiRequestManager.getInstance(this.context).sendLocation(location, new BarikoiTraceLocationUpdateCallback() {
                    @Override
                    public void onlocationUpdate(Location location) {

                    }

                    @Override
                    public void onFailure(BarikoiTraceError barikoiError) {
                        BarikoiTraceLogView.onFailure(barikoiError);
                    }
                });
            }
        } catch (Exception e) {
            throw new BarikoiTraceException(e);
        }
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

            if (!this.storageManager.isSdkTracking() ) {
                return;
            }
            if (!C0089a.m410a(this.storageManager.getAppstate(), this.storageManager) || (!NetworkChecker.isNetworkAvailable(this.context) && this.storageManager.isOfflineTracking())) {
                stopLocationService();
            } else {
                startLocationService();
            }

    }



    public void startLocationService() {
            if (this.storageManager.isSdkTracking() ) {
                this.context.startService(new Intent(this.context, BarikoiTraceLocationService.class));
            }
    }



    public void stopLocationService() {
            this.context.stopService(new Intent(this.context, BarikoiTraceLocationService.class));
            this.storageManager.m235c();

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
