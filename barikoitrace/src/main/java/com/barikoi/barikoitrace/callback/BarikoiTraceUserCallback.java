package com.barikoi.barikoitrace.callback;

import androidx.annotation.Keep;

import com.barikoi.barikoitrace.models.BarikoiTraceError;
import com.barikoi.barikoitrace.models.BarikoiTraceUser;


@Keep
public interface BarikoiTraceUserCallback {
    void onFailure(BarikoiTraceError barikoiError);

    void onSuccess(BarikoiTraceUser traceUser);
}
