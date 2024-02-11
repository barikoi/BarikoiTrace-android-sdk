package com.barikoi.barikoitrace.models;


import org.json.JSONException;

public final class BarikoiTraceErrors {

    public static BarikoiTraceError jsonResponseError(JSONException e) {
        return new BarikoiTraceError("BK402", "JSON response error: "+ e.getMessage());
    }


    public static BarikoiTraceError noUserError() {
        return new BarikoiTraceError("BK402", "User not created");
    }

    public static BarikoiTraceError noKeyError() {
        return new BarikoiTraceError("BK402", "API key not found, Make sure to initialize BarikoiTrace with API key");
    }
    public static BarikoiTraceError invalidAccuracyError() {
        return new BarikoiTraceError("BK402", "Invalid accuracy");
    }



    public static BarikoiTraceError noDataError() {
        return new BarikoiTraceError("BK402", "Required parameter Data not found");
    }




    public static BarikoiTraceError locationTimeout() {
        return new BarikoiTraceError("BK408", "Location timeout");
    }


    public static BarikoiTraceError LocationNotFound() {
        return new BarikoiTraceError("BK408", "Location not found");
    }


    public static BarikoiTraceError LocationPermissionError() {
        return new BarikoiTraceError("BK408", "Location services error or permissions not granted");
    }

    public static BarikoiTraceError LocationSettingsError() {
        return new BarikoiTraceError("BK407", "Location services not enabled");
    }

    public static BarikoiTraceError MockAppError() {
        return new BarikoiTraceError("BK408", "Mock application is in use");
    }


    public static BarikoiTraceError networkError() {
        return new BarikoiTraceError("BK500", "Network error");
    }


    public static BarikoiTraceError PlayServiceError() {
        return new BarikoiTraceError("BK408", "PlayServices error");
    }


    public static BarikoiTraceError serverError() {
        return new BarikoiTraceError("BK500", "Server error");
    }

    public static BarikoiTraceError tripStateError(){
        return new BarikoiTraceError("BK409", "Trip state Error");
    }

    public static BarikoiTraceError m459z() {
        return new BarikoiTraceError("BK402", "JSON conversion error");
    }
}
