package com.barikoi.barikoitrace.service;

import android.location.Location;

import com.barikoi.barikoitrace.models.BarikoiTraceError;
import com.barikoi.barikoitrace.models.BarikoiTraceLocationInfo;

public interface BarikoiTreaceEventCallback {
    default void onError(BarikoiTraceError barikoiError) {

    }


    default void onLocationReceived(BarikoiTraceLocationInfo barikoiLocationInfo) {

    }

    default void onLocationUpdated(Location location) {

    }
}
