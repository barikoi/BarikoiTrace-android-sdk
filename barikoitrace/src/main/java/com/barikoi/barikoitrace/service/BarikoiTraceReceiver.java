package com.barikoi.barikoitrace.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import androidx.annotation.Keep;

import com.barikoi.barikoitrace.localstorage.ConfigStorageManager;
import com.barikoi.barikoitrace.models.BarikoiTraceError;
import com.barikoi.barikoitrace.models.BarikoiTraceErrors;

import java.util.Objects;

@Keep
public  class BarikoiTraceReceiver extends BroadcastReceiver {

    BarikoiTreaceEventCallback callback;

    public void setEventCallback(BarikoiTreaceEventCallback callback){
        this.callback = callback;
    }

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            try {
                if (Objects.equals(intent.getAction(), "com.barikoi.trace.android.RECEIVED") /*&& context.getPackageName().equals(intent.getExtras().getString("packageName"))*/) {
                    ConfigStorageManager a = ConfigStorageManager.getInstance(context);
                    BarikoiTraceError barikoiError = (BarikoiTraceError) intent.getExtras().getSerializable("error");
                    Location location =  intent.getExtras().getParcelable("location");
                    String event = intent.getExtras().getString("event");
                    if ( location != null && callback!=null && (event != null && event.equals("LOCATION_RECEIEVED"))) {
//                        BarikoiTraceLogView.debugLog(intent.getExtras().toString());
                        callback.onLocationUpdated(location);
                    }

                    if (barikoiError != null && callback!=null) {
                       callback.onError(barikoiError);
                    }
                }
            } catch (Exception e) {
                if (callback!=null) callback.onError( BarikoiTraceErrors.noDataError() );
            }
        }
    }


}
