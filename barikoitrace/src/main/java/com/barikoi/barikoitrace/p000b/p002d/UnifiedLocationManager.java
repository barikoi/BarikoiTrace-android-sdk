package com.barikoi.barikoitrace.p000b.p002d;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;

import androidx.annotation.NonNull;

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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnTokenCanceledListener;
import com.google.android.gms.tasks.Task;


public class UnifiedLocationManager {


    private final Context context;


    private final LocationUpdateListener locationUpdateListener;

    private LocationManager locationManager;

    private FusedLocationProviderClient fusedLocationProviderClient;

    private final LocationCallback googleLocationCallback = new GoogleLocationCallback();

    private final LocationListener nativeLocationListenerimp = new NativeLocationListener();

    class GoogleLocationCallback extends LocationCallback {
        GoogleLocationCallback() {
        }

        public void onLocationAvailability(@NonNull LocationAvailability locationAvailability) {
            super.onLocationAvailability(locationAvailability);
            //UnifiedLocationManager.this.locationUpdateListener.onFailure(BarikoiTraceErrors.LocationSettingsError());
            UnifiedLocationManager.this.locationUpdateListener.onProviderAvailabilityChanged(SystemSettingsManager.checkLocationSettings(UnifiedLocationManager.this.context));
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
    }


    @SuppressLint("MissingPermission")
    private void createGoogleLocationUpdate(ConfigStorageManager aVar, int timeInterval, int smallestDisplacement) {
        if (SystemSettingsManager.checkPermissions(this.context)) {
            LocationRequest locationRequest = new LocationRequest();
            int i3 = C0033c.f81a[TraceMode.DesiredAccuracy.toEnum(aVar.getDesiredAccuracy()).ordinal()];
            if (i3 == 1) {
                locationRequest.setPriority(Priority.PRIORITY_HIGH_ACCURACY);
                locationRequest.setPriority(Priority.PRIORITY_LOW_POWER);
            }
            if (timeInterval > 0) {
                long j = (long) (timeInterval * 1000);
                locationRequest.setInterval(j);
                locationRequest.setFastestInterval(j);
                locationRequest.setMaxWaitTime(j * 5);
                locationRequest.setWaitForAccurateLocation(true);
            } else {
                locationRequest.setInterval(0);
                locationRequest.setFastestInterval(0);
                locationRequest.setSmallestDisplacement((float) smallestDisplacement);
                locationRequest.setWaitForAccurateLocation(true);
            }
            FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this.context);
            this.fusedLocationProviderClient = fusedLocationProviderClient;
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, this.googleLocationCallback, Looper.getMainLooper());
            return;
        }
        this.locationUpdateListener.onFailure(BarikoiTraceErrors.LocationPermissionError());

    }

    @SuppressLint("MissingPermission")
    private void createGoogleLocationUpdate(ConfigStorageManager aVar, int timeInterval, int smallestDisplacement, int maxWaitTime) {
        if (SystemSettingsManager.checkPermissions(this.context)) {
            LocationRequest locationRequest = new LocationRequest();
            int i3 = C0033c.f81a[TraceMode.DesiredAccuracy.toEnum(aVar.getDesiredAccuracy()).ordinal()];
            if (i3 == 1) {
                locationRequest.setPriority(Priority.PRIORITY_HIGH_ACCURACY);
                locationRequest.setPriority(Priority.PRIORITY_LOW_POWER );
            }
            if (timeInterval > 0) {
                long j = (long) (timeInterval * 1000);
                locationRequest.setInterval(j);
                locationRequest.setFastestInterval(j);
                if(maxWaitTime>0)locationRequest.setMaxWaitTime(maxWaitTime);
                locationRequest.setWaitForAccurateLocation(true);
            } else {
                locationRequest.setInterval(0);
                locationRequest.setFastestInterval(0);
                locationRequest.setSmallestDisplacement((float) smallestDisplacement);
                locationRequest.setWaitForAccurateLocation(true);
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


    /*public void startLocationUpdate(ConfigStorageManager configStorageManager, int minTime, int minDistance) {
        if (SystemSettingsManager.isGoogleAvailable(this.context)) {
            createGoogleLocationUpdate(configStorageManager, minTime, minDistance);
        } else {
            nativeLocationUpdate(configStorageManager, minTime, minDistance);
        }
    }
*/
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
            int i3 = C0033c.f81a[TraceMode.DesiredAccuracy.toEnum(configStorageManager.getDesiredAccuracy()).ordinal()];
            String str = i3 != 2 ? i3 != 3 ? "gps" : "passive" : "network";
            LocationManager locationManager = (LocationManager) this.context.getSystemService(Context.LOCATION_SERVICE);
            this.locationManager = locationManager;
            if (minTime > 0) {
                locationManager.requestLocationUpdates(str, minTime * 1000L, 0.0f, this.nativeLocationListenerimp, Looper.getMainLooper());
            } else {
                locationManager.requestLocationUpdates(str, 0, (float) minDistance, this.nativeLocationListenerimp, Looper.getMainLooper());
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
                    return null;
                }

                @Override
                public boolean isCancellationRequested() {
                    return false;
                }
            }).addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if(task.isSuccessful() && task.getResult()!=null){
                        Location location = task.getResult();
                        if(location != null){
                            singlelocationlistener.onLocationReceived(location);
                        }
                    }
                }
            });
        }
    }
    @SuppressLint("MissingPermission")
    private void oneTimeNativeLocationUpodate(final LocationUpdateListener singlelocationlistener ){
        if(SystemSettingsManager.checkPermissions(this.context)){
            this.locationManager = (LocationManager) this.context.getSystemService(Context.LOCATION_SERVICE);
            this.locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    singlelocationlistener.onLocationReceived(location);
                }

                @Override
                public void onStatusChanged(String s, int i, Bundle bundle) {

                }

                @Override
                public void onProviderEnabled(String s) {

                }

                @Override
                public void onProviderDisabled(String s) {

                }
            }, Looper.getMainLooper());
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
