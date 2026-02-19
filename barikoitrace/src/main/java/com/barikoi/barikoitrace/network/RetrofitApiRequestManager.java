package com.barikoi.barikoitrace.network;

import android.content.Context;
import android.location.Location;
import android.text.TextUtils;
import android.util.Log;

import com.barikoi.barikoitrace.TraceMode;
import com.barikoi.barikoitrace.callback.BarikoiTraceBulkUpdateCallback;
import com.barikoi.barikoitrace.callback.BarikoiTraceGetTripCallback;
import com.barikoi.barikoitrace.callback.BarikoiTraceLocationUpdateCallback;
import com.barikoi.barikoitrace.callback.BarikoiTraceSettingsCallback;
import com.barikoi.barikoitrace.callback.BarikoiTraceTripApiCallback;
import com.barikoi.barikoitrace.callback.BarikoiTraceUserCallback;
import com.barikoi.barikoitrace.localstorage.ConfigStorageManager;
import com.barikoi.barikoitrace.models.BarikoiTraceError;
import com.barikoi.barikoitrace.models.BarikoiTraceErrors;
import com.barikoi.barikoitrace.models.BarikoiTraceUser;
import com.barikoi.barikoitrace.models.createtrip.Trip;
import com.barikoi.barikoitrace.network.dto.*;
import com.barikoi.barikoitrace.utils.DateTimeUtils;
import com.google.gson.JsonArray;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import org.json.JSONArray;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RetrofitApiRequestManager {
    private static String TAG = "trace_api";
    private static RetrofitApiRequestManager INSTANCE;
    private final ConfigStorageManager configStorageManager;
    private final RetrofitClient retrofitClient;
    private String id, key, base_url;

    public static synchronized RetrofitApiRequestManager getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new RetrofitApiRequestManager(context);
        }
        return INSTANCE;
    }

    private RetrofitApiRequestManager(Context context) {
        this.retrofitClient = RetrofitClient.getInstance(context.getApplicationContext());
        this.configStorageManager = ConfigStorageManager.getInstance(context.getApplicationContext());
        this.id = configStorageManager.getUserID();
        this.key = configStorageManager.getApiKey();
        this.base_url = configStorageManager.getBaseUrl() == null ? Api.base_url : configStorageManager.getBaseUrl();
    }

    public void setBaseURL(String url) {
        base_url = url;
        retrofitClient.setBaseUrl(url);
    }

    public void setorCreateUser(final String name, final String email, final String phone, final BarikoiTraceUserCallback callback) {
        key = configStorageManager.getApiKey();
        AuthenticateRequest request = new AuthenticateRequest(key, name, email, phone);

        retrofitClient.getApiService().authenticate(request).enqueue(new Callback<AuthenticateResponse>() {
            @Override
            public void onResponse(Call<AuthenticateResponse> call, Response<AuthenticateResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        AuthenticateResponse.UserResponse userJson = response.body().getUser();
                        Log.d("userjson", "User retrieved");
                        BarikoiTraceUser user = new BarikoiTraceUser.Builder()
                                .setUserId(userJson.getId())
                                .setName(userJson.getName())
                                .setEmail(userJson.getEmail())
                                .setPhone(phone)
                                .setGroup(userJson.getCompanies()[0].getGroupId())
                                .setCompanyId(userJson.getCompanies()[0].getCompanyId())
                                .build();
                        Log.d("user", user.getGroup());
                        configStorageManager.setUser(user);
                        setId(id);
                        callback.onSuccess(user);
                    } catch (Exception e) {
                        Log.e("userlogerror", e.toString());
                        callback.onFailure(new BarikoiTraceError("BK402", "JSON response error: " + e.getMessage()));
                    }
                } else {
                    callback.onFailure(BarikoiTraceErrors.serverError());
                }
            }

            @Override
            public void onFailure(Call<AuthenticateResponse> call, Throwable t) {
                Log.d(TAG, t.toString());
                callback.onFailure(handleError(t));
            }
        });
    }

    public void sendLocation(final Location location, final BarikoiTraceLocationUpdateCallback callback) {
        final double latitude = location.getLatitude();
        final double longitude = location.getLongitude();
        final double altitude = location.getAltitude();
        final float bearing = location.getBearing();
        final float speed = location.getSpeed();
        final float accuracy = location.getAccuracy();
        final String timestring = DateTimeUtils.getDateTimeLocal(location.getTime());

        if (latitude == 0 || longitude == 0) {
            callback.onFailure(BarikoiTraceErrors.LocationNotFound());
            return;
        }

        LocationRequest request = new LocationRequest(key, id, latitude, longitude, altitude, speed, bearing, timestring, accuracy);

        retrofitClient.getApiService().sendLocation(request).enqueue(new Callback<BaseResponse>() {
            @Override
            public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int status = response.body().getStatus();
                    if (status == 200) {
                        callback.onlocationUpdate(location);
                    } else {
                        String msg = response.body().getMessage();
                        callback.onFailure(new BarikoiTraceError(status + "", msg));
                    }
                } else {
                    callback.onFailure(BarikoiTraceErrors.serverError());
                }
            }

            @Override
            public void onFailure(Call<BaseResponse> call, Throwable t) {
                Log.d(TAG, "error:" + t.getMessage());
                callback.onFailure(handleError(t));
            }
        });
    }

    public void sendOfflineData(final JSONArray data, final BarikoiTraceBulkUpdateCallback callback) {
        JsonArray jsonArray = new JsonArray();
        Gson gson = new Gson();
        for (int i = 0; i < data.length(); i++) {
            Object obj = data.opt(i);
            if (obj != null) {
                JsonElement element = gson.toJsonTree(obj);
                jsonArray.add(element);
            }
        }

        BulkLocationRequest request = new BulkLocationRequest(key, id, jsonArray);

        retrofitClient.getApiService().sendOfflineData(request).enqueue(new Callback<BaseResponse>() {
            @Override
            public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int status = response.body().getStatus();
                    if (status == 200) {
                        callback.onBulkUpdate();
                    } else {
                        String msg = response.body().getMessage();
                        callback.onFailure(new BarikoiTraceError(status + "", msg));
                    }
                } else {
                    callback.onFailure(BarikoiTraceErrors.serverError());
                }
            }

            @Override
            public void onFailure(Call<BaseResponse> call, Throwable t) {
                Log.d(TAG, "error:" + t.getMessage());
                callback.onFailure(handleError(t));
            }
        });
    }

    public void startTrip(final String startTime, final TraceMode tracemode, final String tag, final BarikoiTraceTripApiCallback callback) {
        StartTripRequest request = new StartTripRequest(key, id, startTime, tag, tracemode.isInDebugMode());

        retrofitClient.getApiService().startTrip(request).enqueue(new Callback<TripResponse>() {
            @Override
            public void onResponse(Call<TripResponse> call, Response<TripResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    TripResponse tripResponse = response.body();
                    Log.d("tripstart", "Response received");
                    if ("success".equals(tripResponse.getStatus())) {
                        Trip trip = convertToTrip(tripResponse.getTrip());
                        callback.onSuccess(trip);
                    } else {
                        String msg = tripResponse.getError();
                        callback.onFailure(new BarikoiTraceError(tripResponse.getStatus(), msg));
                    }
                } else {
                    callback.onFailure(BarikoiTraceErrors.serverError());
                }
            }

            @Override
            public void onFailure(Call<TripResponse> call, Throwable t) {
                callback.onFailure(handleError(t));
            }
        });
    }

    public void syncOfflineTrip(final Trip trip, final BarikoiTraceTripApiCallback callback) {
        HashMap<String, String> params = new HashMap<>();
        params.put("api_key", key);
        params.put("user_id", id);
        params.put("start_time", trip.getStart_time());
        params.put("end_time", trip.getEnd_time());
        if (trip.getTag() != null) params.put("tag", trip.getTag());
        params.put("state", trip.getState() + "");

        retrofitClient.getApiService().syncOfflineTrip(params).enqueue(new Callback<BaseResponse>() {
            @Override
            public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int status = response.body().getStatus();
                    if (status == 200 || status == 201) {
                        callback.onSuccess(trip);
                    } else {
                        String msg = response.body().getMessage();
                        callback.onFailure(new BarikoiTraceError(status + "", msg));
                    }
                } else {
                    callback.onFailure(BarikoiTraceErrors.serverError());
                }
            }

            @Override
            public void onFailure(Call<BaseResponse> call, Throwable t) {
                Log.d("BarikoiTraceTrip", "error:" + t.getMessage());
                callback.onFailure(handleError(t));
            }
        });
    }

    public void endTrip(final String endTime, final BarikoiTraceTripApiCallback callback) {
        EndTripRequest request = new EndTripRequest(key, id, endTime);

        retrofitClient.getApiService().endTrip(request).enqueue(new Callback<TripResponse>() {
            @Override
            public void onResponse(Call<TripResponse> call, Response<TripResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    TripResponse tripResponse = response.body();
                    Log.d("tripend", tripResponse.toString());
                    if ("success".equals(tripResponse.getStatus())) {
                        callback.onSuccess(convertToTrip(tripResponse.getTrip()));
                    } else {
                        String msg = tripResponse.getError();
                        callback.onFailure(new BarikoiTraceError(tripResponse.getStatus(), msg));
                    }
                } else {
                    callback.onFailure(BarikoiTraceErrors.serverError());
                }
            }

            @Override
            public void onFailure(Call<TripResponse> call, Throwable t) {
                if (t != null) Log.d("BarikoiTraceTrip", "error:" + t.getMessage());
                callback.onFailure(handleError(t));
            }
        });
    }

    public void getCurrentTrip(final BarikoiTraceGetTripCallback callback) {
        HashMap<String, String> params = new HashMap<>();
        params.put("api_key", key);
        params.put("user_id", id);

        retrofitClient.getApiService().getCurrentTrip(params).enqueue(new Callback<ActiveTripResponse>() {
            @Override
            public void onResponse(Call<ActiveTripResponse> call, Response<ActiveTripResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    boolean active = response.body().isActive();
                    if (active) {
                        Trip trip = convertToTrip(response.body().getTrip());
                        callback.onSuccess(trip);
                    } else {
                        callback.onSuccess(null);
                    }
                } else {
                    callback.onFailure(BarikoiTraceErrors.serverError());
                }
            }

            @Override
            public void onFailure(Call<ActiveTripResponse> call, Throwable t) {
                if (t != null) Log.d("BarikoiTraceTrip", "error:" + t.getMessage());
                callback.onFailure(handleError(t));
            }
        });
    }

    public void syncSettings(BarikoiTraceUser user, final BarikoiTraceSettingsCallback callback) {
        if (user == null) {
            callback.onFailure(BarikoiTraceErrors.noUserError());
            return;
        }
        if (TextUtils.isEmpty(user.getPhone())) {
            callback.onFailure(BarikoiTraceErrors.noUserError());
            return;
        }

        CompanySettingsRequest request = new CompanySettingsRequest(key, user.getPhone());

        retrofitClient.getApiService().syncSettings(request).enqueue(new Callback<CompanySettingsResponse>() {
            @Override
            public void onResponse(Call<CompanySettingsResponse> call, Response<CompanySettingsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        CompanySettingsResponse.SettingsData settings = response.body().getSettings();
                        TraceMode mode = new TraceMode.Builder()
                                .setUpdateInterval(settings.getUpdateTimeInterval())
                                .setDistancefilter(settings.getDistanceInterval())
                                .setAccuracyFilter(settings.getAccuracyFilter())
                                .setOfflineSync(settings.isOfflineSync())
                                .setStartTime(java.time.LocalTime.parse(settings.getTrackingStartTime()))
                                .setEndTime(java.time.LocalTime.parse(settings.getTrackingEndTime()))
                                .build();
                        configStorageManager.setTraceModeWithTiming(mode);
                        callback.onSuccess(mode);
                    } catch (Exception e) {
                        callback.onFailure(new BarikoiTraceError("BK402", "JSON response error: " + e.getMessage()));
                    }
                } else {
                    callback.onFailure(BarikoiTraceErrors.serverError());
                }
            }

            @Override
            public void onFailure(Call<CompanySettingsResponse> call, Throwable t) {
                if (t != null) Log.d("BarikoiTraceTrip", "error:" + t.getMessage());
                callback.onFailure(handleError(t));
            }
        });
    }

    public void insertLogFile(final String path, final BarikoiTraceBulkUpdateCallback callback) {
        File file = new File(path);
        if (!file.exists()) {
            callback.onFailure(BarikoiTraceErrors.serverError());
            return;
        }

        RequestBody userIdBody = RequestBody.create(MediaType.parse("text/plain"), id);
        MultipartBody.Part logFile = MultipartBody.Part.createFormData("log", file.getName(),
                RequestBody.create(MediaType.parse("text/plain"), getFileData(path)));

        retrofitClient.getApiService().uploadLogFile(userIdBody, logFile).enqueue(new Callback<BaseResponse>() {
            @Override
            public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                if (response.isSuccessful()) {
                    callback.onBulkUpdate();
                } else {
                    callback.onFailure(BarikoiTraceErrors.serverError());
                }
            }

            @Override
            public void onFailure(Call<BaseResponse> call, Throwable t) {
                callback.onFailure(handleError(t));
            }
        });
    }

    private byte[] getFileData(String f) {
        File textFile = new File(f);
        int size = (int) textFile.length();
        byte[] bytes = new byte[size];
        byte[] tmpBuff = new byte[size];

        try (FileInputStream inputStream = new FileInputStream(textFile)) {
            int read = inputStream.read(bytes, 0, size);
            if (read < size) {
                int remain = size - read;
                while (remain > 0) {
                    read = inputStream.read(tmpBuff, 0, remain);
                    System.arraycopy(tmpBuff, 0, bytes, size - remain, read);
                    remain -= read;
                }
            }
        } catch (IOException e) {
            Log.w("TraceLog", "Error reading file", e);
        }
        return bytes;
    }

    private Trip convertToTrip(TripResponse.TripData tripData) {
        if (tripData == null) return null;
        String id = tripData.getId() != null ? tripData.getId() : "";
        String startTime = tripData.getStartTime();
        String endTime = tripData.getEndTime();
        String tag = tripData.getTag();
        int state = tripData.getStatus();
        String userId = tripData.getUserId() != null ? tripData.getUserId() : "";
        return new Trip(id, startTime, endTime, tag, state, userId, 1);
    }

    private void setId(String id) {
        INSTANCE.id = id;
    }

    public void setKey(String key) {
        INSTANCE.key = key;
    }

    private BarikoiTraceError handleError(Throwable t) {
        if (t == null) {
            return BarikoiTraceErrors.serverError();
        } else if (t instanceof IOException) {
            return BarikoiTraceErrors.networkError();
        }
        return BarikoiTraceErrors.serverError();
    }
}
