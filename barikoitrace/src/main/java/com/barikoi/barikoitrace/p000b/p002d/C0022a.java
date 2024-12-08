package com.barikoi.barikoitrace.p000b.p002d;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.barikoi.barikoitrace.TraceMode;
import com.barikoi.barikoitrace.utils.NetworkChecker;
import com.barikoi.barikoitrace.utils.SystemSettingsManager;
import com.barikoi.barikoitrace.models.BarikoiTraceError;
import com.barikoi.barikoitrace.models.BarikoiTraceErrors;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;


public class C0022a {


    private final Context context;


    private Location location = null;


    private final LocationUpdateListener locationUpdateListener;


    private FusedLocationProviderClient fusedLocationProviderClient;


    private LocationManager locationManager;


    private Handler handler = new Handler(Looper.getMainLooper());


    private final int f61g;


   // private final ConfigStorageManager configStorageManager;


    private final LocationCallback locationCallback = new C0025c();


    private final LocationListener locationListener = new C0026d();


    private Runnable f65k = new RunnableC0027e();



    public class C0023a implements OnFailureListener {
        C0023a() {
        }

        public void onFailure(@NonNull Exception exc) {
            C0022a.this.m106a(BarikoiTraceErrors.noDataError());
        }
    }



    public static class C0024b implements OnSuccessListener<Location> {
        C0024b() {
        }


        public void onSuccess(Location location) {
           /* if (location == null || location.getLatitude() == 0.0d || location.getLongitude() == 0.0d) {
                C0022a.this.m106a(BarikoiTraceErrors.noDataError());
            } else if ((!SystemSettingsManager.m374a(C0022a.this.context, location) || !C0022a.this.configStorageManager.isMock()) && SystemSettingsManager.m374a(C0022a.this.context, location)) {
                C0022a.this.m106a(BarikoiTraceErrors.MockAppError());
            } else {
                location.setSpeed(0.0f);
                C0022a.this.m115e();
                C0022a.this.locationUpdateListener.onLocationReceived(location);
            }*/
        }
    }


    static class C0025c extends LocationCallback {
        C0025c() {
        }

        public void onLocationAvailability(@NonNull LocationAvailability locationAvailability) {
            /*C0022a.super.onLocationAvailability(locationAvailability);
            if (!SystemSettingsManager.checkLocationSettings(C0022a.this.context)) {
                C0022a.this.m106a(BarikoiTraceErrors.LocationPermissionError());
            }*/
        }

        public void onLocationResult(@NonNull LocationResult locationResult) {
//            for (Location location : locationResult.getLocations()) {
//               /* if ((!SystemSettingsManager.m374a(C0022a.this.context, location) || !C0022a.this.configStorageManager.isMock()) && SystemSettingsManager.m374a(C0022a.this.context, location)) {
//                    C0022a.this.m106a(BarikoiTraceErrors.MockAppError());
//                } else {
//                    C0022a.this.m103a(location);
//                }*/
//            }
        }
    }


    class C0026d implements LocationListener {
        C0026d() {
        }

        @Override // android.location.LocationListener
        public void onLocationChanged(@NonNull Location location) {
            /*if ((!SystemSettingsManager.m374a(C0022a.this.context, location) || !C0022a.this.configStorageManager.isMock()) && SystemSettingsManager.m374a(C0022a.this.context, location)) {
                C0022a.this.m106a(BarikoiTraceErrors.MockAppError());
            } else {
                C0022a.this.m103a(location);
            }*/
        }

        @Override // android.location.LocationListener
        public void onProviderDisabled(@NonNull String str) {
            if (!SystemSettingsManager.checkLocationSettings(C0022a.this.context)) {
                C0022a.this.m106a(BarikoiTraceErrors.LocationPermissionError());
            }
        }

        @Override // android.location.LocationListener
        public void onProviderEnabled(@NonNull String str) {
        }

        @Override // android.location.LocationListener
        public void onStatusChanged(String str, int i, Bundle bundle) {
        }
    }


    class RunnableC0027e implements Runnable {
        RunnableC0027e() {
        }

        @Override // java.lang.Runnable
        public void run() {
            C0022a.this.m115e();
            if (C0022a.this.location == null || C0022a.this.location.getLatitude() == 0.0d || C0022a.this.location.getLongitude() == 0.0d) {
                C0022a.this.locationUpdateListener.onFailure(BarikoiTraceErrors.locationTimeout());
            } else {
                C0022a.this.locationUpdateListener.onLocationReceived(C0022a.this.location);
            }
        }
    }



    public static  class C0028f {


        static final  int[] f71a;

        static {
            int[] iArr = new int[TraceMode.DesiredAccuracy.values().length];
            f71a = iArr;
            try {
                iArr[TraceMode.DesiredAccuracy.HIGH.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                f71a[TraceMode.DesiredAccuracy.MEDIUM.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                f71a[TraceMode.DesiredAccuracy.LOW.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    public C0022a(Context context, LocationUpdateListener bVar, int i) {
        this.context = context;
        this.locationUpdateListener = bVar;
        this.f61g = i;
    }




    private void m103a(Location location) {
        location.setSpeed(0.0f);
        this.location = location;
        if (location.getAccuracy() <= ((float) this.f61g)) {
            m109c();
            m115e();
            this.locationUpdateListener.onLocationReceived(location);
        }
    }




    private void m106a(BarikoiTraceError barikoiError) {
        m109c();
        m115e();
        this.locationUpdateListener.onFailure(barikoiError);
    }


    private void m108b(TraceMode.DesiredAccuracy desiredAccuracy) {
        if (SystemSettingsManager.checkPermissions(this.context)) {
            LocationRequest locationRequest = new LocationRequest();
            int i = C0028f.f71a[desiredAccuracy.ordinal()];
            if (i == 1) {
                locationRequest.setPriority(100);
            } else if (i == 2) {
                locationRequest.setPriority(102);
            } else if (i == 3) {
                locationRequest.setPriority(104);
            }
            locationRequest.setInterval(1000);
            locationRequest.setFastestInterval(1000);
            this.handler.postDelayed(this.f65k, 30000);
            FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this.context);
            this.fusedLocationProviderClient = fusedLocationProviderClient;
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, this.locationCallback, Looper.getMainLooper());
            return;
        }
        this.locationUpdateListener.onFailure(BarikoiTraceErrors.LocationPermissionError());
    }


    private void m109c() {
        Runnable runnable;
        Handler handler = this.handler;
        if (handler != null && (runnable = this.f65k) != null) {
            handler.removeCallbacks(runnable);
            this.handler = null;
            this.f65k = null;
        }
    }


    @SuppressLint("MissingPermission")
    private void m110c(TraceMode.DesiredAccuracy desiredAccuracy) {
        if (SystemSettingsManager.checkPermissions(this.context)) {
            int i = C0028f.f71a[desiredAccuracy.ordinal()];
            String str = i != 2 ? i != 3 ? "gps" : "passive" : "network";
            this.handler.postDelayed(this.f65k, 30000);
            LocationManager locationManager = (LocationManager) this.context.getSystemService(Context.LOCATION_SERVICE);
            this.locationManager = locationManager;
            locationManager.requestLocationUpdates(str, 0, 0.0f, this.locationListener, Looper.getMainLooper());
            return;
        }
        this.locationUpdateListener.onFailure(BarikoiTraceErrors.LocationPermissionError());
    }


    @SuppressLint("MissingPermission")
    private void m113d() {
        if (SystemSettingsManager.checkPermissions(this.context)) {
            LocationManager locationManager = (LocationManager) this.context.getSystemService(Context.LOCATION_SERVICE);
            this.locationManager = locationManager;


            Location lastKnownLocation = locationManager.getLastKnownLocation("gps");
            if (lastKnownLocation == null || lastKnownLocation.getLatitude() == 0.0d || lastKnownLocation.getLongitude() == 0.0d) {
                m106a(BarikoiTraceErrors.noDataError());
            }  else {
                lastKnownLocation.setSpeed(0.0f);
                m115e();
                this.locationUpdateListener.onLocationReceived(lastKnownLocation);
            }
        } else {
            this.locationUpdateListener.onFailure(BarikoiTraceErrors.LocationPermissionError());
        }
    }




    private void m115e() {
        LocationManager locationManager = this.locationManager;
        if (locationManager != null) {
            locationManager.removeUpdates(this.locationListener);
        }
        FusedLocationProviderClient fusedLocationProviderClient = this.fusedLocationProviderClient;
        if (fusedLocationProviderClient != null) {
            fusedLocationProviderClient.removeLocationUpdates(this.locationCallback);
        }
    }


    public void m116a() {
        if (SystemSettingsManager.checkPermissions(this.context)) {
            FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this.context);
            this.fusedLocationProviderClient = fusedLocationProviderClient;
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new C0024b()).addOnFailureListener(new C0023a());
            return;
        }
        this.locationUpdateListener.onFailure(BarikoiTraceErrors.LocationPermissionError());
    }


    public void m117a(TraceMode.DesiredAccuracy desiredAccuracy) {
        if (!SystemSettingsManager.isGoogleAvailable(this.context) || !NetworkChecker.isNetworkAvailable(this.context)) {
            m110c(desiredAccuracy);
        } else {
            m108b(desiredAccuracy);
        }
    }


    public void m118b() {
        if (SystemSettingsManager.isGoogleAvailable(this.context)) {
            m116a();
        } else {
            m113d();
        }
    }
}
