package com.barikoi.barikoitrace.models;

import androidx.annotation.Keep;

import java.io.Serializable;

@Keep
public class BarikoiTraceError implements Serializable {
    private String code;
    private String message;

    public BarikoiTraceError(String str, String str2) {
        this.code = str;
        this.message = str2;
    }

    public String getCode() {
        return this.code;
    }

    public String getMessage() {
        return this.message;
    }
}
