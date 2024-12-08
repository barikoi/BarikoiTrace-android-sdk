package com.barikoi.barikoitrace.service;

import static android.content.Context.NOTIFICATION_SERVICE;
import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.location.Location;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.app.NotificationCompat;
import androidx.work.ForegroundInfo;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.barikoi.barikoitrace.R;
import com.barikoi.barikoitrace.callback.BarikoiTraceLocationUpdateCallback;
import com.barikoi.barikoitrace.exceptions.BarikoiTraceException;
import com.barikoi.barikoitrace.localstorage.sqlitedb.LocationDbHelper;
import com.barikoi.barikoitrace.models.BarikoiTraceError;
import com.barikoi.barikoitrace.network.ApiRequestManager;
import com.barikoi.barikoitrace.network.JsonResponseAdapter;
import com.barikoi.barikoitrace.p000b.p002d.LocationUpdateListener;
import com.barikoi.barikoitrace.p000b.p002d.UnifiedLocationManager;
import com.google.common.util.concurrent.ListenableFuture;

public class LocationWork extends ListenableWorker {

    private final Context mContext;

    public LocationWork(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        mContext = context;
    }


    @NonNull
    public ListenableFuture<Result> startWork() {

        Log.d("locationworkmanager", "doWork: Started to work");
        setForegroundAsync(createForegroundInfo("Syncing Location"));
        return CallbackToFutureAdapter.getFuture(completer -> {
            UnifiedLocationManager unifiedLocationManager = new UnifiedLocationManager(mContext, new LocationUpdateListener() {
                @Override
                public void onLocationReceived(Location location) {
                    ApiRequestManager.getInstance(getApplicationContext()).sendLocation(location, new BarikoiTraceLocationUpdateCallback() {
                        @Override
                        public void onlocationUpdate(Location location) {
                            Log.d("locationtask", "location update success");
                            completer.set(Result.success());
                        }

                        @Override
                        public void onFailure(BarikoiTraceError barikoiError) {
                            Log.d("locationtask", "location update failure");
                            try {
                                LocationDbHelper.getInstance(mContext).insertLocation(JsonResponseAdapter.getlocationJson(location));
                            } catch (BarikoiTraceException e) {
                                Log.e("locationtask", "location update failure", e);
                            }
                            completer.set(Result.failure());
                        }
                    });
                }

                @Override
                public void onFailure(BarikoiTraceError barikoiError) {
                    completer.set(Result.failure());
                }

                @Override
                public void onProviderAvailabilityChanged(boolean available) {

                }
            });
            unifiedLocationManager.oneTimeLocationUpdate();
            return completer;
        });
    }


    @NonNull
    private ForegroundInfo createForegroundInfo(@NonNull String progress) {
        // Build a notification using bytesRead and contentLength

        Context context = getApplicationContext();
        String id = "barikoi_channel_sync_location";
        String title = "Location Syncing...";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel();
        }

        Notification notification = new NotificationCompat.Builder(context, id)
                .setContentTitle(title)
                .setTicker(title)
                .setSmallIcon(R.drawable.barikoi_logo)
                .setOngoing(true)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager. TYPE_NOTIFICATION ))
                // Add the cancel action to the notification which can
                // be used to cancel the worker
                .build();

        return new ForegroundInfo(36999, notification, FOREGROUND_SERVICE_TYPE_LOCATION);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createChannel() {
        // Create a Notification channel
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager. TYPE_NOTIFICATION );

        NotificationChannel channel = new NotificationChannel("barikoi_channel_sync_location", "Location Syncing", NotificationManager.IMPORTANCE_LOW);
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build();
        channel.setSound(soundUri, audioAttributes);
        ((NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

    }

}