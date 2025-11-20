package com.barikoi.barikoitrace.p000b.p002d;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.barikoi.barikoitrace.TraceMode;
import com.barikoi.barikoitrace.utils.SystemSettingsManager;
import com.barikoi.barikoitrace.localstorage.ConfigStorageManager;
import com.barikoi.barikoitrace.models.BarikoiTraceErrors;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.OnTokenCanceledListener;


public class UnifiedLocationManager {


    private final Context context;

    private Location lastLocation;

    private final LocationUpdateListener locationUpdateListener;

    private LocationManager locationManager;

    private FusedLocationProviderClient fusedLocationProviderClient;

    private final LocationCallback googleLocationCallback = new GoogleLocationCallback();

    private final LocationListener nativeLocationListenerimp = new NativeLocationListener();

    class GoogleLocationCallback extends LocationCallback {
        GoogleLocationCallback() {
        }

        @Override
        public void onLocationAvailability(@NonNull LocationAvailability locationAvailability) {
            super.onLocationAvailability(locationAvailability);
            UnifiedLocationManager.this.locationUpdateListener.onProviderAvailabilityChanged(SystemSettingsManager.checkLocationSettings(UnifiedLocationManager.this.context));
        }

        @Override
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
        public void onLocationChanged(@NonNull Location location) {
            if ((!SystemSettingsManager.checkifMockprovider(UnifiedLocationManager.this.context, location) ) && SystemSettingsManager.checkifMockprovider(UnifiedLocationManager.this.context, location)) {
                UnifiedLocationManager.this.locationUpdateListener.onFailure(BarikoiTraceErrors.MockAppError());
            } else {
                UnifiedLocationManager.this.locationUpdateListener.onLocationReceived(location);
            }
        }

        @Override // android.location.LocationListener
        public void onProviderDisabled(@NonNull String str) {
            if (!SystemSettingsManager.checkLocationSettings(UnifiedLocationManager.this.context)) {
                UnifiedLocationManager.this.locationUpdateListener.onProviderAvailabilityChanged(false);
            }
        }

        @Override // android.location.LocationListener
        public void onProviderEnabled(@NonNull String str) {
            UnifiedLocationManager.this.locationUpdateListener.onProviderAvailabilityChanged(true);
        }

    }



    public UnifiedLocationManager(Context context, LocationUpdateListener bVar) {
        this.context = context;
        this.locationUpdateListener = bVar;
    }



    @SuppressLint("MissingPermission")
    private void createGoogleLocationUpdate(ConfigStorageManager aVar, int timeInterval, int smallestDisplacement, int maxWaitTime) {
        if (SystemSettingsManager.checkPermissions(this.context)) {
            LocationRequest.Builder locationRequest = new LocationRequest.Builder(timeInterval*1000L);
            TraceMode.DesiredAccuracy accuracy = TraceMode.DesiredAccuracy.toEnum(aVar.getDesiredAccuracy());
            if (accuracy.equals(TraceMode.DesiredAccuracy.HIGH)) {
                locationRequest.setPriority(Priority.PRIORITY_HIGH_ACCURACY);
            }
            if (timeInterval > 0) {
                long j = timeInterval * 1000L;
                locationRequest.setIntervalMillis(j);
                locationRequest.setMinUpdateIntervalMillis(j);
                if(maxWaitTime>0)locationRequest.setMaxUpdateDelayMillis(maxWaitTime);
                locationRequest.setWaitForAccurateLocation(true);
            } else {
//                locationRequest.setIntervalMillis(0);
                locationRequest.setMinUpdateDistanceMeters(smallestDisplacement);
                locationRequest.setWaitForAccurateLocation(true);
            }
            this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this.context);
            fusedLocationProviderClient.requestLocationUpdates(locationRequest.build(), this.googleLocationCallback, Looper.getMainLooper());
            return;
        }
        this.locationUpdateListener.onFailure(BarikoiTraceErrors.LocationPermissionError());
    }

    public void removeLocationUpdate() {
        if (locationManager != null) {
            locationManager.removeUpdates(this.nativeLocationListenerimp);
        }
        if (fusedLocationProviderClient != null) {
            fusedLocationProviderClient.removeLocationUpdates(this.googleLocationCallback);
        }
    }


    public void startLocationUpdate(ConfigStorageManager configStorageManager, int minTime, int minDistance, int pingSyncInterval) {
        if (SystemSettingsManager.isGoogleAvailable(this.context)) {
            createGoogleLocationUpdate(configStorageManager, minTime, minDistance, pingSyncInterval);
        } else {
            nativeLocationUpdate(configStorageManager, minTime, minDistance);
        }
    }

    @SuppressLint("MissingPermission")
    private void nativeLocationUpdate(ConfigStorageManager configStorageManager, int minTime, int minDistance) {
        if (SystemSettingsManager.checkPermissions(this.context)) {
            TraceMode.DesiredAccuracy accuracy = TraceMode.DesiredAccuracy.toEnum(configStorageManager.getDesiredAccuracy());
            String str = switch (accuracy) {
                case HIGH -> "gps";
                case MEDIUM -> "network";
                case LOW -> "passive";
            };
            this.locationManager = (LocationManager) this.context.getSystemService(Context.LOCATION_SERVICE);
            if (minTime > 0) {
                locationManager.requestLocationUpdates(str, minTime * 1000L, 0.0f, this.nativeLocationListenerimp, Looper.getMainLooper());
            } else {
                locationManager.requestLocationUpdates(str, 0, minDistance, this.nativeLocationListenerimp, Looper.getMainLooper());
            }
        } else {
            this.locationUpdateListener.onFailure(BarikoiTraceErrors.LocationPermissionError());
        }
    }
    @SuppressLint("MissingPermission")
    private void oneTimeGoogleLocationUpdate(final LocationUpdateListener singlelocationlistener ){
        if(SystemSettingsManager.checkPermissions(this.context)){
            if(fusedLocationProviderClient==null)
                this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this.context);

            fusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, new CancellationToken() {
                @NonNull
                @Override
                public CancellationToken onCanceledRequested(@NonNull OnTokenCanceledListener onTokenCanceledListener) {
                    return this;
                }

                @Override
                public boolean isCancellationRequested() {
                    return false;
                }
            }).addOnCompleteListener(task -> {
                if(task.isSuccessful() && task.getResult()!=null){
                    Location location = task.getResult();
                    if(location != null){
                        singlelocationlistener.onLocationReceived(location);
                    }
                }
            });
        }
    }
    @SuppressLint("MissingPermission")
    private void oneTimeNativeLocationUpodate(final LocationUpdateListener singlelocationlistener ){
        if(SystemSettingsManager.checkPermissions(this.context)){
            this.locationManager = (LocationManager) this.context.getSystemService(Context.LOCATION_SERVICE);


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                this.locationManager.getCurrentLocation(LocationManager.GPS_PROVIDER,
                        null,
                        ContextCompat.getMainExecutor(this.context),
                        singlelocationlistener::onLocationReceived);
            }
            else {
                this.locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER,
                        singlelocationlistener::onLocationReceived,
                        Looper.getMainLooper());
            }
        }
    }

    public void oneTimeLocationUpdate(){
        if (SystemSettingsManager.isGoogleAvailable(this.context)) {
            oneTimeGoogleLocationUpdate(locationUpdateListener);
        } else {
            oneTimeNativeLocationUpodate(locationUpdateListener);
        }
    }

}
