package com.barikoi.barikoitrace.network;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class UploadLogRequestBody {
    private final RequestBody userId;
    private final MultipartBody.Part logPart;

    public UploadLogRequestBody(String userId, MultipartBody.Part logPart) {
        this.userId = RequestBody.create(okhttp3.MediaType.parse("text/plain"), userId);
        this.logPart = logPart;
    }

    public RequestBody getUserId() {
        return userId;
    }

    public MultipartBody.Part getLogPart() {
        return logPart;
    }
}
