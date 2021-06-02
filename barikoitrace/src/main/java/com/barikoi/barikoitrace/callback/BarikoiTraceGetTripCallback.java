package com.barikoi.barikoitrace.callback;

import com.barikoi.barikoitrace.models.BarikoiTraceError;
import com.barikoi.barikoitrace.models.createtrip.Trip;

import java.util.ArrayList;

public interface BarikoiTraceGetTripCallback {

    void onSuccess(ArrayList<Trip> trips);
    void onFailure(BarikoiTraceError barikoiError);

}
