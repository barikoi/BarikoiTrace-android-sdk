package com.barikoi.barikoitrace.p000b.p002d;

import android.location.Location;

import com.barikoi.barikoitrace.models.BarikoiTraceError;


public interface LocationUpdateListener {

    void onLocationReceived(Location location);

    void onFailure(BarikoiTraceError barikoiError);
}
