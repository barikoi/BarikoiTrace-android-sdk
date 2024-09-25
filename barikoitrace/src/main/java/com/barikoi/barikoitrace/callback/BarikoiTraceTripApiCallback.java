package com.barikoi.barikoitrace.callback;

import com.barikoi.barikoitrace.models.BarikoiTraceError;
import com.barikoi.barikoitrace.models.createtrip.Trip;

public interface BarikoiTraceTripApiCallback {
    void onFailure(BarikoiTraceError barikoiError);

    void onSuccess(Trip trip);
}
