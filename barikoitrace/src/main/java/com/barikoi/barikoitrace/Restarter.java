package com.barikoi.barikoitrace;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

public class Restarter extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i("Broadcast Listened", "Service tried to stop");
		//Toast.makeText(context, "Service restarted", Toast.LENGTH_SHORT).show();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String token = prefs.getString("token", "");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			context.startForegroundService(new Intent(context, TimerService.class).putExtra("token",token));
		} else {
			//Intent background = new Intent(context, TimerService.class);
			context.startService(new Intent(context, TimerService.class).putExtra("token",token));
			//context.startService(new Intent(context, TimerService.class));
		}


	}
}