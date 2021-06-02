package com.barikoi.barikoitrace.models;


public final class BarikoiTraceErrors {

    public static BarikoiTraceError jsonResponseError() {
        return new BarikoiTraceError("BK402", "JSON response error");
    }


    public static BarikoiTraceError noUserError() {
        return new BarikoiTraceError("BK402", "User not created");
    }


    public static BarikoiTraceError invalidAccuracyError() {
        return new BarikoiTraceError("BK402", "Invalid accuracy");
    }



    public static BarikoiTraceError noDataError() {
        return new BarikoiTraceError("BK402", "Data not found");
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
