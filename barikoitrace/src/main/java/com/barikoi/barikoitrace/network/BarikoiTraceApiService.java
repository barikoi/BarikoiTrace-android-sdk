package com.barikoi.barikoitrace.network;

import com.barikoi.barikoitrace.network.dto.*;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.QueryMap;

import java.util.Map;

public interface BarikoiTraceApiService {

    @POST("sdk/authenticate")
    Call<AuthenticateResponse> authenticate(@Body AuthenticateRequest request);

    @POST("sdk/add-gpx")
    Call<BaseResponse> sendLocation(@Body LocationRequest request);

    @POST("sdk/bulk-gpx")
    Call<BaseResponse> sendOfflineData(@Body BulkLocationRequest request);

    @POST("trip/create")
    Call<TripResponse> startTrip(@Body StartTripRequest request);

    @POST("trip/end")
    Call<TripResponse> endTrip(@Body EndTripRequest request);

    @GET("trip/check-active-trip")
    Call<ActiveTripResponse> getCurrentTrip(@QueryMap Map<String, String> queryParams);

    @POST("trip/offline")
    Call<BaseResponse> syncOfflineTrip(@QueryMap Map<String, String> queryParams);

    @POST("sdk/company/settings")
    Call<CompanySettingsResponse> syncSettings(@Body CompanySettingsRequest request);

    @Multipart
    @POST("app/log")
    Call<BaseResponse> uploadLogFile(@Part("user_id") RequestBody userId, @Part MultipartBody.Part logFile);
}
