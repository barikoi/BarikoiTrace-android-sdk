package com.barikoi.barikoitrace.callback;

import androidx.annotation.Keep;

import com.barikoi.barikoitrace.models.BarikoiTraceError;

@Keep
public interface BarikoiTraceLogoutCallback {
    void onFailure(BarikoiTraceError barikoiError);

    void onSuccess(String str);
}
