package com.barikoi.barikoitrace;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by Sakib on 6/25/2017.
 */

public class LocationTask {
    public int REQUEST_CHECK_SETTINGS = 0x2;
    public int REQUEST_CHECK_PERMISSION = 0x1;
    private final LocationRequest mLocationRequest;

    private final LocationCallback mLocationCallback;
    private LocationTaskListener listener;
    Context context;

    private FusedLocationProviderClient mFusedLocationClient;
    private static final String TAG="LocationTask";
    private Handler handler;



    public LocationTask(final Context context, LocationTaskListener listener){
        this.context=context;
        this.listener=listener;

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(3000);
        mLocationRequest.setSmallestDisplacement(50);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationAvailability(LocationAvailability locationAvailability) {
                super.onLocationAvailability(locationAvailability);
                onLocationAvailable(locationAvailability.isLocationAvailable());

            }

            @Override
            public void onLocationResult(LocationResult locationResult) {
                onLocationChanged(locationResult.getLastLocation());
                /*for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    onLocationChanged(location);
                }*/
            };
        };
    }





    public void displayLocation() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION}, REQUEST_CHECK_PERMISSION);

        } else {
            checkForLocationsettings();
        }
    }

    public void checkForLocationsettings() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        LocationServices.getSettingsClient(context).checkLocationSettings(builder.build())
                .addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
                    @Override
                    public void onComplete(Task<LocationSettingsResponse> task) {
                        try {
                            LocationSettingsResponse response = task.getResult(ApiException.class);
                            // All location settings are satisfied. The client can initialize location
                            getLastLocation();

                        } catch (ApiException exception) {
                            switch (exception.getStatusCode()) {
                                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                    // Location settings are not satisfied. But could be fixed by showing the
                                    // user a dialog.
                                    try {
                                        // Cast to a resolvable exception.
                                        ResolvableApiException resolvable = (ResolvableApiException) exception;
                                        // Show the dialog by calling startResolutionForResult(),
                                        // and check the result in onActivityResult().
                                        resolvable.startResolutionForResult(
                                                (Activity) context,
                                                REQUEST_CHECK_SETTINGS);
                                    } catch (IntentSender.SendIntentException e) {
                                        // Ignore the error.
                                    } catch (ClassCastException e) {
                                        // Ignore, should be an impossible error.
                                    }
                                    break;
                                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                    // Location settings are not satisfied. However, we have no way to fix the
                                    // settings so we won't show the dialog.
                                    //mGoogleApiClient.disconnect();
                                    break;
                            }
                        }
                    }
                });

    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Intent intent = new Intent();
            context.startActivity(intent);
            return;
        }

        Log.d("locationupdate","getLastLocation");

        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                mLocationCallback,
                null /* Looper */);
//        handler= new Handler();
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                stoptask();
//            }
//        }, 10000);
    }

    public void showEnableLocationSetting() {
        final int LOCATION_SETTING_REQUEST = 999;

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        Task<LocationSettingsResponse> task = LocationServices.getSettingsClient(context)
                .checkLocationSettings(builder.build());

        task.addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse response) {
                LocationSettingsStates states = response.getLocationSettingsStates();
                if (states.isLocationPresent()) {
                    //Do something
                }
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                int statusCode = ((ApiException) e).getStatusCode();
                if (statusCode == CommonStatusCodes.RESOLUTION_REQUIRED) {
                    try {
                        // Handle result in onActivityResult()
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult((Activity) context, LOCATION_SETTING_REQUEST);
                    } catch (IntentSender.SendIntentException sendEx) { }
                }
            }
        });

    }
    public void stoptask(){
        Log.d("locationupdate","stopTask");
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        //mGoogleApiClient.disconnect();
        handler.removeCallbacksAndMessages(null);
    }

    public void getAddress(final double lat, final double lon){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        final String token = prefs.getString(Api.TOKEN, "");
        RequestQueue queue = RequestQueueSingleton.getInstance(context.getApplicationContext()).getRequestQueue();
        StringRequest request = new StringRequest(Request.Method.GET,
                Api.url_base+"reverse?latitude="+lat+"&longitude="+lon,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if(context instanceof Activity){
                            Activity container=(Activity)context;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                                if(container.isDestroyed() || container.isFinishing()) return;
                            }
                        }
                       /* try {
                            JSONObject place=new JSONArray(response).getJSONObject(0);
                            String address=place.getString("Address")+", "+place.getString("area")+", "+place.getString("city");
                            Place p=new Place(place.getString("Address"),lon+"",lat+"","",place.getString("city"),place.getString("area"),"","","");
                            float distance=Float.parseFloat(place.getString("distance"))*1000;
                            if(distance>50){
                                getaddressG(lat,lon);
                            }
                            else if(p!=null && listener!=null ) listener.reversedAddress(p);

                        } catch (JSONException e) {
                            e.printStackTrace();
                            //Toast.makeText(SelectPlaceActivity.this,"Problem with the server",Toast.LENGTH_LONG).show();
                            getaddressG(lat,lon);
                        }*/
                    }

                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //loading.setVisibility(View.GONE);
                        //NetworkcallUtils.handleResponse(error,context);
                    }
                }
        ) {
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> parameters = new HashMap<String, String>();
                //parameters.put("id", id.getText().toString());
                if(!token.equals("")) {
                    parameters.put("Authorization", "bearer " + token);
                }
                return parameters;
            }

        };
        queue.add(request);
    }
    public void sendLocationData(final Location location,final int id, final String apikey){
//        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
//        final String token = prefs.getString(Api.TOKEN, "");
        final String time = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ENGLISH).format(location.getTime());
        RequestQueue queue = RequestQueueSingleton.getInstance(context.getApplicationContext()).getRequestQueue();
        String reqUrl=Api.gpx_url+"?latitude="+location.getLatitude()+"&longitude="+location.getLongitude()+"&speed="+location.getSpeed()+"&bearing="+location.getBearing()+"&altitude="+location.getAltitude()+"&gpx_time="+time+"&api_key="+apikey+"&user_id="+id;
        Log.d(TAG,reqUrl);
        StringRequest request = new StringRequest(Request.Method.POST,
                reqUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
						//Toast.makeText(context.getApplicationContext(), "sendLocation", Toast.LENGTH_SHORT).show();

                        Log.d("locationupdate", +location.getLatitude() +" " +location.getLongitude()+ " time: " +time);
                        Log.d("locationupdate","okay");
                        //Toast.makeText(context, "gps live", Toast.LENGTH_SHORT).show();
                    }

                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("locationupdate","error:"+error.getMessage());
                        //loading.setVisibility(View.GONE);
                        //Toast.makeText(context, "problem", Toast.LENGTH_SHORT).show();
                        //NetworkcallUtils.handleResponse(error,context);
                    }
                }
        ) ;
        request.setRetryPolicy(new DefaultRetryPolicy(40 * 1000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        request.setShouldCache(false);
        queue.add(request);
    }
    public void onLocationChanged(Location location) {
        /*stoptask();
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);*/

        /*getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);*/

        //mGoogleApiClient.disconnect();

        //Toast.makeText(context.getApplicationContext(), "distance: " +location, Toast.LENGTH_LONG).show();
        if(listener!=null)
            //Toast.makeText(context.getApplicationContext(), "listener: " +listener, Toast.LENGTH_LONG).show();
            listener.OnLocationChanged(location);
    }

    public void onLocationAvailable(boolean available){
        if(listener!=null)
            listener.onLocationAvailability(available);
    }

}