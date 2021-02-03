package com.barikoi.barikoitrace.callback;

import android.location.Location;

import com.barikoi.barikoitrace.models.BarikoiTraceError;

public interface BarikoiTraceLocationUpdateCallback {
    void onlocationUpdate(Location location);

    void onFailure(BarikoiTraceError barikoiError);
}
