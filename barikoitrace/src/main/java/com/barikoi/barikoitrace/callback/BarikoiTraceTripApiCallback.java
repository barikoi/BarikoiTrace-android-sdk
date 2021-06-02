package com.barikoi.barikoitrace.callback;

import com.barikoi.barikoitrace.models.BarikoiTraceError;

public interface BarikoiTraceTripApiCallback {
    void onFailure(BarikoiTraceError barikoiError);

    void onSuccess();
}
