package com.barikoi.barikoitrace.p000b.p002d;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;

import com.barikoi.barikoitrace.TraceMode;
import com.barikoi.barikoitrace.Utils.SystemSettingsManager;
import com.barikoi.barikoitrace.localstorage.ConfigStorageManager;
import com.barikoi.barikoitrace.models.BarikoiTraceErrors;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;


public class UnifiedLocationManager {


    private Context context;


    private LocationUpdateListener locationUpdateListener;

    private LocationManager locationManager;

    private FusedLocationProviderClient fusedLocationProviderClient;

    private ConfigStorageManager configStorageManager;

    private LocationCallback googleLocationCallback = new GoogleLocationCallback();

    private LocationListener nativeLocationListenerimp = new NativeLocationListener();

    class GoogleLocationCallback extends LocationCallback {
        GoogleLocationCallback() {
        }

        public void onLocationAvailability(LocationAvailability locationAvailability) {
            super.onLocationAvailability(locationAvailability);
            if (!SystemSettingsManager.checkLocationSettings(UnifiedLocationManager.this.context)) {
                UnifiedLocationManager.this.locationUpdateListener.onProviderAvailabilityChanged(false);
                //UnifiedLocationManager.this.locationUpdateListener.onFailure(BarikoiTraceErrors.LocationSettingsError());
            }else{
                UnifiedLocationManager.this.locationUpdateListener.onProviderAvailabilityChanged(true);

            }
        }

        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                if ((!SystemSettingsManager.checkifMockprovider(UnifiedLocationManager.this.context, location) ) && SystemSettingsManager.checkifMockprovider(UnifiedLocationManager.this.context, location)) {
                    UnifiedLocationManager.this.locationUpdateListener.onFailure(BarikoiTraceErrors.MockAppError());
                } else {
                    UnifiedLocationManager.this.locationUpdateListener.onLocationReceived(location);
                }
            }
        }
    }


    class NativeLocationListener implements LocationListener {
        NativeLocationListener() {
        }

        @Override // android.location.LocationListener
        public void onLocationChanged(Location location) {
            if ((!SystemSettingsManager.checkifMockprovider(UnifiedLocationManager.this.context, location) ) && SystemSettingsManager.checkifMockprovider(UnifiedLocationManager.this.context, location)) {
                UnifiedLocationManager.this.locationUpdateListener.onFailure(BarikoiTraceErrors.MockAppError());
            } else {
                UnifiedLocationManager.this.locationUpdateListener.onLocationReceived(location);
            }
        }

        @Override // android.location.LocationListener
        public void onProviderDisabled(String str) {
            if (!SystemSettingsManager.checkLocationSettings(UnifiedLocationManager.this.context)) {
                UnifiedLocationManager.this.locationUpdateListener.onProviderAvailabilityChanged(false);
                //UnifiedLocationManager.this.locationUpdateListener.onFailure(BarikoiTraceErrors.LocationSettingsError());
            }
        }

        @Override // android.location.LocationListener
        public void onProviderEnabled(String str) {
            UnifiedLocationManager.this.locationUpdateListener.onProviderAvailabilityChanged(true);
        }

        @Override // android.location.LocationListener
        public void onStatusChanged(String str, int i, Bundle bundle) {
        }
    }



    public static  class C0033c {


        static final  int[] f81a;

        static {
            int[] iArr = new int[TraceMode.DesiredAccuracy.values().length];
            f81a = iArr;
            try {
                iArr[TraceMode.DesiredAccuracy.HIGH.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                f81a[TraceMode.DesiredAccuracy.MEDIUM.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                f81a[TraceMode.DesiredAccuracy.LOW.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    public UnifiedLocationManager(Context context, LocationUpdateListener bVar) {
        this.context = context;
        this.locationUpdateListener = bVar;
        this.configStorageManager = ConfigStorageManager.getInstance(context);
    }


    @SuppressLint("MissingPermission")
    private void createGoogleLocationUpdate(ConfigStorageManager aVar, int timeInterval, int smallestDisplacement) {
        if (SystemSettingsManager.checkPermissions(this.context)) {
            LocationRequest locationRequest = new LocationRequest();
            int i3 = C0033c.f81a[TraceMode.DesiredAccuracy.toEnum(aVar.getDesiredAccuracy()).ordinal()];
            if (i3 == 1) {
                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            } else if (i3 == 2) {
                locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
            } else if (i3 == 3) {
                locationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER );
            }
            if (timeInterval > 0) {
                long j = (long) (timeInterval * 1000);
                locationRequest.setInterval(j);
                locationRequest.setFastestInterval(j);
            } else {
                locationRequest.setInterval(0);
                locationRequest.setFastestInterval(0);
                locationRequest.setSmallestDisplacement((float) smallestDisplacement);
            }
            FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this.context);
            this.fusedLocationProviderClient = fusedLocationProviderClient;
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, this.googleLocationCallback, Looper.getMainLooper());
            return;
        }
        this.locationUpdateListener.onFailure(BarikoiTraceErrors.LocationPermissionError());
    }


    public void removeLocationUpdate() {
        LocationManager locationManager = this.locationManager;
        if (locationManager != null) {
            locationManager.removeUpdates(this.nativeLocationListenerimp);
        }
        FusedLocationProviderClient fusedLocationProviderClient = this.fusedLocationProviderClient;
        if (fusedLocationProviderClient != null) {
            fusedLocationProviderClient.removeLocationUpdates(this.googleLocationCallback);
        }
    }


    public void startLocationUpdate(ConfigStorageManager configStorageManager, int minTime, int minDistance) {
        if (SystemSettingsManager.isGoogleAvailable(this.context)) {
            createGoogleLocationUpdate(configStorageManager, minTime, minDistance);
        } else {
            nativeLocationUpdate(configStorageManager, minTime, minDistance);
        }
    }


    @SuppressLint("MissingPermission")
    public void nativeLocationUpdate(ConfigStorageManager configStorageManager, int minTime, int minDistance) {
        if (SystemSettingsManager.checkPermissions(this.context)) {
            int i3 = C0033c.f81a[TraceMode.DesiredAccuracy.toEnum(configStorageManager.getDesiredAccuracy()).ordinal()];
            String str = i3 != 2 ? i3 != 3 ? "gps" : "passive" : "network";
            LocationManager locationManager = (LocationManager) this.context.getSystemService(Context.LOCATION_SERVICE);
            this.locationManager = locationManager;
            if (minTime > 0) {
                locationManager.requestLocationUpdates(str, (long) (minTime * 1000), 0.0f, this.nativeLocationListenerimp, Looper.getMainLooper());
            } else {
                locationManager.requestLocationUpdates(str, 0, (float) minDistance, this.nativeLocationListenerimp, Looper.getMainLooper());
            }
        } else {
            this.locationUpdateListener.onFailure(BarikoiTraceErrors.LocationPermissionError());
        }
    }

}
