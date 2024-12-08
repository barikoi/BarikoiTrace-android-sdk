package com.barikoi.barikoitrace.callback;


import com.barikoi.barikoitrace.models.BarikoiTraceError;

public interface BarikoiTraceBulkUpdateCallback {
    void onBulkUpdate();
    void onFailure(BarikoiTraceError barikoiError);
}
