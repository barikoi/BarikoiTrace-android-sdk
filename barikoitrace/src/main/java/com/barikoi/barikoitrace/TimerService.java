package com.barikoi.barikoitrace;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static androidx.core.app.NotificationCompat.PRIORITY_LOW;
import static androidx.core.app.NotificationCompat.PRIORITY_MAX;
import static androidx.core.app.NotificationCompat.PRIORITY_MIN;
import static com.barikoi.barikoitrace.BarikoiTrace.APIKEY_TAG;
import static com.barikoi.barikoitrace.BarikoiTrace.BARIKOI_ID_TAG;

public class TimerService extends Service {
    // constant
    public static final long NOTIFY_INTERVAL = 60* 1000; // 60 seconds

    // run on another Thread to avoid crash
    private Handler mHandler = new Handler();
    // timer handling
    private Timer mTimer = null;
    private LocationTask lt;
    private String apikey;
	public int userid=0;
    private boolean isFirst;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        // cancel if already existed
		String CHANNEL_ID = "Barikoi Tracking App";
		String CHANNEL_NAME = "Barikoi Tracking App is running as Background service. Do not turn off.";

		NotificationChannel channel = null;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			channel = new NotificationChannel(CHANNEL_ID,
					CHANNEL_NAME, NotificationManager.IMPORTANCE_NONE);
			((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

			Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
					.setCategory(Notification.CATEGORY_SERVICE)
					.setContentText(CHANNEL_NAME)
					.setSmallIcon(R.drawable.barikoi_logo)
					.setPriority(PRIORITY_MAX)
					.build();
			startForeground(1, notification);
		}else{
//            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this,CHANNEL_ID)
//                    .setSmallIcon(R.drawable.barikoi_logo)
//                    .setContentTitle(CHANNEL_ID)
//                    .setContentText(CHANNEL_NAME)
//                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            Notification notification2 =  new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setCategory(Notification.CATEGORY_SERVICE)
					.setContentText(CHANNEL_NAME)
					.setSmallIcon(R.drawable.barikoi_logo)
					.setPriority(PRIORITY_MAX)
					.build();

			//NotificationManager notifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

			//notifyMgr.notify(101, notification2);
            //managerCompat.notify(1,mBuilder.build());
            startForeground(1, notification2);
        }

//		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
//			startMyOwnForeground();
//		else
//			startForeground(1, new Notification());

    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null && intent.hasExtra(APIKEY_TAG)) {
			apikey = intent.getStringExtra(APIKEY_TAG);
		}
        if (intent != null && intent.hasExtra(BARIKOI_ID_TAG)) {
            userid = intent.getIntExtra(BARIKOI_ID_TAG,0);
        }


        lt=new LocationTask(this, new LocationTaskListener() {
            @Override
            public void onLocationAvailability( boolean available) {

            }

            @Override
            public void oneEndTask() {

            }

            @Override
            public void OnLocationChanged(Location location) {
//
                Log.d("locationupdate","service on");
                lt.sendLocationData(location, userid,apikey);
            }
        });
		lt.displayLocation();
        // schedule task

      //startTimer();
        return START_STICKY;
    }

    public static double getKmFromLatLong(double lat1, double lng1, double lat2, double lng2){
        Location loc1 = new Location("");
        loc1.setLatitude(lat1);
        loc1.setLongitude(lng1);
        Location loc2 = new Location("");
        loc2.setLatitude(lat2);
        loc2.setLongitude(lng2);
        double distanceInMeters = loc1.distanceTo(loc2);
        return distanceInMeters;
    }

	public static boolean isMyServiceRunning(Context context, Class<?> serviceClass) {
		ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);

		if (services != null) {
			for (int i = 0; i < services.size(); i++) {
				if ((serviceClass.getName()).equals(services.get(i).service.getClassName()) && services.get(i).pid != 0) {
					return true;
				}
			}
		}
		return false;
	}

//	@Override
//	public void onTaskRemoved(Intent rootIntent) {
//		super.onTaskRemoved(rootIntent);
//		Log.d("locationupdate","service removed");
//		stopForeground(true);
//	}

//	@Override
//	public void onDestroy() {
//		super.onDestroy();
//		stoptimertask();
//
//		Intent broadcastIntent = new Intent();
//		broadcastIntent.setAction("restartservice");
//		broadcastIntent.setClass(this, Restarter.class);
//		this.sendBroadcast(broadcastIntent);
//	}

//	private Timer timer;
//	private TimerTask timerTask;
//	public void startTimer() {
//		timer = new Timer();
//		timerTask = new TimerTask() {
//			public void run() {
//				Log.i("Count", "=========  "+ (counter++));
//			}
//		};
//		timer.schedule(timerTask, 1000, 1000); //
//	}

//	public void stoptimertask() {
//		if (timer != null) {
//			timer.cancel();
//			timer = null;
//		}
//	}



	class TimeDisplayTimerTask extends TimerTask {

        @Override
        public void run() {
            // run on another thread
            mHandler.post(new Runnable() {

                @Override
                public void run() {
//                    lt.displayLocation();
                }

            });
        }

    }

    }