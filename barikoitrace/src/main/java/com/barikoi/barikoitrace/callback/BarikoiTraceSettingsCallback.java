package com.barikoi.barikoitrace.callback;

import com.barikoi.barikoitrace.TraceMode;
import com.barikoi.barikoitrace.models.BarikoiTraceError;

public interface BarikoiTraceSettingsCallback {
    void onFailure(BarikoiTraceError barikoiError);

    void onSuccess(TraceMode tracemode);
}
