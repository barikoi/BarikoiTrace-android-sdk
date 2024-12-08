package com.barikoi.barikoitrace.callback;

import com.barikoi.barikoitrace.models.BarikoiTraceError;
import com.barikoi.barikoitrace.models.createtrip.Trip;


public interface BarikoiTraceGetTripCallback {

    void onSuccess(Trip trip);
    void onFailure(BarikoiTraceError barikoiError);

}
