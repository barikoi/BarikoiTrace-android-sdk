package com.barikoi.barikoitrace.Utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.barikoi.barikoitrace.p000b.LocationTracker;


public class NetworkChangeManager {


    private Context context;


    private LocationTracker locationTracker;


    private ConnectivityManager.NetworkCallback networkCallback;


    private BroadcastReceiver broadcastReceiver;


    @RequiresApi(api = 21)

    public class NetworkChangeCallback extends ConnectivityManager.NetworkCallback {
        private NetworkChangeCallback() {
        }

        @Override // android.net.SystemSettingsManager.NetworkCallback
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
            try {

                //NetworkChangeManager.this.locationTracker.m83c();
            } catch (Exception e) {
            }
        }

        @Override // android.net.SystemSettingsManager.NetworkCallback
        public void onLost(@NonNull Network network) {
            super.onLost(network);
            try {

                //NetworkChangeManager.this.locationTracker.m83c();
            } catch (Exception e) {
            }
        }
    }



    public class NetworkChangeReceiver extends BroadcastReceiver {
        private NetworkChangeReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            try {
                if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {

                    //NetworkChangeManager.this.locationTracker.m83c();
                }
            } catch (Exception e) {
            }
        }
    }

    public NetworkChangeManager(Context context) {
        this.context = context;
        this.locationTracker = new LocationTracker(context);
    }


    public void registerReceiver() {
        try {
            if (Build.VERSION.SDK_INT >= 24) {
                NetworkChangeCallback bVar = new NetworkChangeCallback();
                this.networkCallback = bVar;
                ((ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE)).registerDefaultNetworkCallback(bVar);
                return;
            }
            IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            NetworkChangeReceiver cVar = new NetworkChangeReceiver();
            this.broadcastReceiver = cVar;
            this.context.registerReceiver(cVar, intentFilter);
        } catch (Exception e) {
        }
    }


    public void unregisterReceiver() {
        try {
            if (Build.VERSION.SDK_INT >= 21) {
                ((ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE)).unregisterNetworkCallback(this.networkCallback);
            } else {
                this.context.unregisterReceiver(this.broadcastReceiver);
            }
        } catch (Exception e) {
        }
    }
}
