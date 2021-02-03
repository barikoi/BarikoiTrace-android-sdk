package com.barikoi.barikoitrace.callback;

import android.location.Location;

import androidx.annotation.Keep;

import com.barikoi.barikoitrace.models.BarikoiTraceError;


@Keep
public interface BarikoiTraceLocationCallback {
    void location(Location location);

    void onFailure(BarikoiTraceError barikoiError);
}
