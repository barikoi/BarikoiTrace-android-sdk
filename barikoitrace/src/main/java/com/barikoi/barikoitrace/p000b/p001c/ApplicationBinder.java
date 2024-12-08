package com.barikoi.barikoitrace.p000b.p001c;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.barikoi.barikoitrace.localstorage.ConfigStorageManager;



public class ApplicationBinder implements Application.ActivityLifecycleCallbacks {


    private int f51a = 0;


    private Context context;


    private ConfigStorageManager configStorageManager;


    public ApplicationBinder(Context context, ConfigStorageManager aVar) {
        this.context = context;
        this.configStorageManager = aVar;
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
//            if (!TextUtils.isEmpty(this.configStorageManager.getUserID())) {
//                //BarikoiTraceClient.getInstance(this.context).initialize(null, null);
//            }
            DeviceInfo.updateBatteryInfo(this.context);
//            this.context.startService(new Intent(this.context, BarikoiTraceService.class));
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
