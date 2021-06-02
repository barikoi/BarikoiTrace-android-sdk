package com.barikoi.barikoitrace.p000b;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.barikoi.barikoitrace.TraceMode;
import com.barikoi.barikoitrace.Utils.SystemSettingsManager;
import com.barikoi.barikoitrace.event.BootEventReceiver;
import com.barikoi.barikoitrace.exceptions.BarikoiTraceException;
import com.barikoi.barikoitrace.localstorage.ConfigStorageManager;
import com.barikoi.barikoitrace.localstorage.sqlitedb.LogDbHelper;
import com.barikoi.barikoitrace.models.BarikoiTraceErrors;
import com.barikoi.barikoitrace.models.C0089a;
import com.barikoi.barikoitrace.network.RequestQueueSingleton;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;

import static com.android.volley.Request.Method.POST;

/* renamed from: com.geospark.lib.b.a */
public final class GeofenceManager {

    /* renamed from: f */
    private static GeofenceManager INSTANCE;

    /* renamed from: a */
    private Context f39a;

    /* renamed from: b */
    private ConfigStorageManager f40b;

    /* renamed from: c */
    private GeofencingClient geoclient;

    /* renamed from: d */
    private LocationTracker locationTracker;

    /* renamed from: e */
    private LogDbHelper logDbHelper;

    private GeofenceManager(Context context) {
        this.f39a = context;
        this.logDbHelper = LogDbHelper.getInstance(context);
        this.f40b = ConfigStorageManager.getInstance(context);
        this.locationTracker = new LocationTracker(context);
        this.geoclient = LocationServices.getGeofencingClient(context);
    }
    /* renamed from: a */
    private PendingIntent m65a(Context context) {
        Intent intent = new Intent(context, BootEventReceiver.class);
        intent.setAction("barikoitrace.barikoitracereceiver.GEOFENCE");
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /* renamed from: b */
    public static GeofenceManager getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new GeofenceManager(context.getApplicationContext());
        }
        return INSTANCE;
    }

    /* access modifiers changed from: package-private */
    /* renamed from: a */
    public void removeGeofences() {
        this.geoclient.removeGeofences(m65a(this.f39a));
    }

    /* renamed from: a */
    public void onGeofenceEventReceived(Intent intent) throws BarikoiTraceException {
        try {
            Toast.makeText(f39a.getApplicationContext(), "geofence event received", Toast.LENGTH_SHORT).show();
            GeofencingEvent fromIntent = GeofencingEvent.fromIntent(intent);
            Location triggeringLocation = fromIntent.getTriggeringLocation();
            if (fromIntent.getGeofenceTransition() ==Geofence.GEOFENCE_TRANSITION_ENTER ) {
                sendNotification("Geofence"+fromIntent.getTriggeringGeofences().get(0).getRequestId()+": Enter event from "+triggeringLocation.toString());

            }else if (fromIntent.getGeofenceTransition() ==Geofence.GEOFENCE_TRANSITION_DWELL ) {
                sendNotification("Geofence"+fromIntent.getTriggeringGeofences().get(0).getRequestId()+": stayed in the geofence ");


            }else if(fromIntent.getGeofenceTransition()== Geofence.GEOFENCE_TRANSITION_EXIT){
                sendNotification("Geofence"+fromIntent.getTriggeringGeofences().get(0).getRequestId()+": Eext event from "+triggeringLocation.toString());
            }

        } catch (Exception e) {
            throw new BarikoiTraceException(e);
        }
    }

    private void sendNotification(String text){
        try {
            JsonObjectRequest req = new JsonObjectRequest(POST, "https://hooks.slack.com/services/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", new JSONObject().put("text",text),
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {

                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d("barikoitraceerror",error.getMessage());
                }
            } );
            RequestQueueSingleton.getInstance().getRequestQueue().add(req);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
    /* renamed from: a */
    @SuppressLint("MissingPermission")
    public void createGeofence(double lat,double lon , int radius, int delay, String id) throws BarikoiTraceException {
        try {
            //this.logDbHelper.m312a("Geofence radius/delay: " + i + "/" + i2);
            if (!SystemSettingsManager.checkPermissions(this.f39a)) {
                this.locationTracker.sendLocationBroadCast((Location) null, (String) null, BarikoiTraceErrors.LocationPermissionError());
            } else if (lat != 0) {
                removeGeofences();

                this.geoclient.addGeofences(
                        new GeofencingRequest.Builder().addGeofence(
                                new Geofence.Builder().setRequestId(id)
                                        .setCircularRegion(lat, lon, (float) radius)
                                        .setExpirationDuration(1500000)
                                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER|Geofence.GEOFENCE_TRANSITION_DWELL|Geofence.GEOFENCE_TRANSITION_EXIT)
                                        .setLoiteringDelay(delay * 10000).build())
                                .build(), m65a(this.f39a)).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(f39a, "geofence created", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(f39a, "geofence failed "+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

            }

        } catch (Exception e) {
            throw new BarikoiTraceException(e);
        }
    }
}
