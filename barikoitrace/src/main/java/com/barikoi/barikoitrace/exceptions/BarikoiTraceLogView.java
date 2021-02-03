package com.barikoi.barikoitrace.exceptions;

import android.util.Log;

import com.barikoi.barikoitrace.models.BarikoiTraceError;


public final class BarikoiTraceLogView {

    public static void onFailure(BarikoiTraceError barikoiError) {
        Log.e(barikoiError.getCode(), barikoiError.getMessage());
    }


    public static void onSuccess(String str) {
        Log.i("BarikoiTraceLog", str);
    }

    public static void debugLog(String msg){
        Log.d("BarikoiTraceLog", msg);
    }
}
