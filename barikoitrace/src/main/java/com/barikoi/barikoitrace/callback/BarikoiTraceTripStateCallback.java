package com.barikoi.barikoitrace.callback;

import com.barikoi.barikoitrace.models.BarikoiTraceError;

public interface BarikoiTraceTripStateCallback {

    void onSuccess();

    void onFailure(BarikoiTraceError barikoiError);

}
