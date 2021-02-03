package com.barikoi.barikoitrace.p000b.p001c;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.barikoi.barikoitrace.localstorage.ConfigStorageManager;
import com.barikoi.barikoitrace.p000b.LocationTracker;
import com.barikoi.barikoitrace.service.BarikoiTraceService;



public class ApplicationBinder implements Application.ActivityLifecycleCallbacks {


    private int f51a = 0;


    private Context context;


    private ConfigStorageManager configStorageManager;


    private LocationTracker locationTracker;

    public ApplicationBinder(Context context, ConfigStorageManager aVar, LocationTracker bVar) {
        this.context = context;
        this.configStorageManager = aVar;
        this.locationTracker = bVar;
    }

    @Override // android.app.Application.ActivityLifecycleCallbacks
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
    }

    @Override // android.app.Application.ActivityLifecycleCallbacks
    public void onActivityDestroyed(@NonNull Activity activity) {
    }

    @Override // android.app.Application.ActivityLifecycleCallbacks
    public void onActivityPaused(@NonNull Activity activity) {
    }

    @Override // android.app.Application.ActivityLifecycleCallbacks
    public void onActivityResumed(@NonNull Activity activity) {
    }

    @Override // android.app.Application.ActivityLifecycleCallbacks
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {
    }

    @Override // android.app.Application.ActivityLifecycleCallbacks
    public void onActivityStarted(@NonNull Activity activity) {
        if (this.f51a == 0) {
            if (!TextUtils.isEmpty(this.configStorageManager.getUserID())) {
                //BarikoiTraceClient.getInstance(this.context).initialize(null, null);
            }
            DeviceInfo.updateBatteryInfo(this.context);
            this.context.startService(new Intent(this.context, BarikoiTraceService.class));
            this.configStorageManager.setAppstate("F");
            //this.locationTracker.m85e();
        }
        this.f51a++;
    }

    @Override // android.app.Application.ActivityLifecycleCallbacks
    public void onActivityStopped(@NonNull Activity activity) {
        int i = this.f51a - 1;
        this.f51a = i;
        if (i == 0) {
            this.configStorageManager.setAppstate("B");
            //this.locationTracker.m85e();
        }
    }


}
