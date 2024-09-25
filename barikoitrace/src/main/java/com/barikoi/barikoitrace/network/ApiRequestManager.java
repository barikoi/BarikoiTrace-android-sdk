package com.barikoi.barikoitrace.network;

import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.barikoi.barikoitrace.TraceMode;
import com.barikoi.barikoitrace.Utils.DateTimeUtils;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ApiRequestManager {
    private static ApiRequestManager INSTANCE;
    private final ConfigStorageManager configStorageManager;
    private final RequestQueue requestQueue;
    private String id, key;
    public static ApiRequestManager getInstance(Context context) {
        if (INSTANCE == null){
            INSTANCE=new ApiRequestManager(context);
        }
        return INSTANCE;
    }

    private ApiRequestManager(Context context){
        this.requestQueue=RequestQueueSingleton.getInstance(context.getApplicationContext()).getRequestQueue();
        configStorageManager=ConfigStorageManager.getInstance(context.getApplicationContext());
        id=configStorageManager.getUserID();
        key=configStorageManager.getApiKey();
    }

    public void setUser(final String email, final String phone, final BarikoiTraceUserCallback callback){
        id=configStorageManager.getUserID();
        key=configStorageManager.getApiKey();
        StringRequest request = new StringRequest(Request.Method.POST,
                Api.user_url,
                response -> {
                    try {
                        JSONObject responsejson=new JSONObject(response);
                        int status= responsejson.getInt("status");
                        if(status==200 || status==201){
                            JSONObject userjson=responsejson.getJSONObject("user");
                            String id= userjson.getString("_id");
                            String name= userjson.getString("name");
                            String email1 =userjson.getString("email");
                            String phone1 =userjson.getString("phone");
                            BarikoiTraceUser user=new BarikoiTraceUser.Builder().setUserId(id).setPhone(phone1).setEmail(email1).build();
                            configStorageManager.setUser(user);
                            callback.onSuccess(user);
                        }else {
                            String msg= responsejson.getString("message");
                            callback.onFailure(new BarikoiTraceError(status+"",msg));
                        }
                    } catch (JSONException e) {

                        callback.onFailure(BarikoiTraceErrors.jsonResponseError(e));
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("locationupdate","error:"+error.getMessage());
                        callback.onFailure(BarikoiTraceErrors.serverError());

                    }
                }
        ) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                HashMap<String,String> params=new HashMap<>();
                params.put("api_key",key);
                if(!TextUtils.isEmpty(email)) params.put("email",email);
                if(!TextUtils.isEmpty(phone)) params.put("phone",phone);
                return new JSONObject(params).toString().getBytes();
            }

        };
        request.setRetryPolicy(new DefaultRetryPolicy(40 * 1000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        request.setShouldCache(false);
        requestQueue.add(request);
    }

    public void setorCreateUser(final String name, final String email, final String phone, final BarikoiTraceUserCallback callback){


        key=configStorageManager.getApiKey();
        StringRequest request = new StringRequest(Request.Method.POST,
                Api.get_create_user_url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject responsejson=new JSONObject(response);
                            int status= responsejson.getInt("status");
                            Log.d("userjson",responsejson.toString());
                            if(status==200 || status==201){
                                JSONObject userjson=responsejson.getJSONObject("user");
                                String id= userjson.getString("_id");
                                String name= userjson.getString("name");
                                String email=userjson.getString("email");
                                String phone=userjson.getString("phone");
                                BarikoiTraceUser user=new BarikoiTraceUser.Builder()
                                        .setUserId(id)
                                        .setPhone(phone)
                                        .setEmail(email)
                                        .setName(name)
                                        .build();
                                configStorageManager.setUser(user);
                                setId(id);
                                callback.onSuccess(user);
                            }else {
                                String msg= responsejson.getString("message");
                                callback.onFailure(new BarikoiTraceError(status+"",msg));
                            }
                        } catch (JSONException e) {
                            Log.e("userlogerror", e.getMessage());
                            callback.onFailure(BarikoiTraceErrors.jsonResponseError(e));
                        }
                    }

                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
//                        Log.d("locationupdate","error:"+error.networkResponse.toString());
                        if (error instanceof NetworkError || error instanceof NoConnectionError || error instanceof  TimeoutError)
                            callback.onFailure(BarikoiTraceErrors.networkError());
                        else if (error.networkResponse!=null && error.networkResponse.data!=null){
                            String errorstring = new String(error.networkResponse.data);
                            try {
                                JSONObject responsejson=new JSONObject(errorstring);
                                String msg= responsejson.getString("error");
                                callback.onFailure(new BarikoiTraceError(error.networkResponse.statusCode+"",msg));
                            } catch (JSONException e) {
                                callback.onFailure(BarikoiTraceErrors.serverError());
                            }
                        }else callback.onFailure(BarikoiTraceErrors.serverError());
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String,String> header= new HashMap<>();
                header.put("Content-Type","application/json");
                return header;
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                HashMap<String,String> params=new HashMap<>();
                params.put("api_key",key);
                if(!TextUtils.isEmpty(name)) params.put("name",name);
                if(!TextUtils.isEmpty(email)) params.put("email",email);
                if(!TextUtils.isEmpty(phone)) params.put("phone",phone);
                return new JSONObject(params).toString().getBytes();
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(40 * 1000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        request.setShouldCache(false);
        requestQueue.add(request);
    }

    public void sendLocation(final Location location, final BarikoiTraceLocationUpdateCallback callback){
        final double latitude= location.getLatitude();
        final double longitude= location.getLongitude();
        final double altitude=location.getAltitude();
        long time=location.getTime();
        final float bearing= location.getBearing();
        final float speed= location.getSpeed();
        final float accuracy=location.getAccuracy();
        final String timestring= DateTimeUtils.getDateTimeLocal(time);

        if(latitude==0 || longitude==0){
            callback.onFailure(BarikoiTraceErrors.LocationNotFound());
        }
        StringRequest request = new StringRequest(Request.Method.POST,
                Api.gpx_url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject responsejson=new JSONObject(response);
                            int status= responsejson.getInt("status");
                            if(status==200){
                                callback.onlocationUpdate(location);
                            }else {
                                String msg= responsejson.getString("message");
                                callback.onFailure(new BarikoiTraceError(status+"",msg));
                            }
                        } catch (JSONException e) {
                            callback.onFailure(BarikoiTraceErrors.jsonResponseError(e));
                        }
                    }

                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("locationupdate","error:"+error.getMessage());
                        if (error instanceof NetworkError || error instanceof NoConnectionError || error instanceof  TimeoutError)
                            callback.onFailure(BarikoiTraceErrors.networkError());
                        else if (error.networkResponse!=null && error.networkResponse.data!=null){
                            String errorstring = new String(error.networkResponse.data);
                            try {
                                JSONObject responsejson=new JSONObject(errorstring);
                                String msg= responsejson.getString("error");
                                callback.onFailure(new BarikoiTraceError(error.networkResponse.statusCode+"",msg));
                            } catch (JSONException e) {
                                callback.onFailure(BarikoiTraceErrors.serverError());
                            }
                        }else callback.onFailure(BarikoiTraceErrors.serverError());
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String,String> header= new HashMap<>();
                header.put("Content-Type","application/json");
                return header;
            }

            @Override
            public byte[] getBody()  throws AuthFailureError {
                HashMap<String,Object> params=new HashMap<>();
                params.put("api_key",key);
                params.put("user_id",id);

                params.put("latitude",latitude);
                params.put("longitude",longitude);
                params.put("altitude",altitude);
                params.put("speed",speed);
                params.put("bearing",bearing);
                params.put("gpx_time",timestring);
                params.put("accuracy",accuracy);
                return new JSONObject(params).toString().getBytes();
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(20 * 1000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        request.setShouldCache(false);
        requestQueue.add(request);
    }
    public void sendOfflineData(final JSONArray data, final BarikoiTraceBulkUpdateCallback callback ){

        StringRequest request = new StringRequest(Request.Method.POST,
                Api.bulk_url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject responsejson=new JSONObject(response);
                            int status= responsejson.getInt("status");
                            if(status==200){
                                callback.onBulkUpdate();
                            }else {
                                String msg= responsejson.getString("message");
                                callback.onFailure(new BarikoiTraceError(status+"",msg));
                            }
                        } catch (JSONException e) {
                            callback.onFailure(BarikoiTraceErrors.jsonResponseError(e));
                        }
                    }

                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("locationupdate","error:"+error.getMessage());
                        if (error instanceof NetworkError || error instanceof NoConnectionError || error instanceof  TimeoutError)
                            callback.onFailure(BarikoiTraceErrors.networkError());
                        else callback.onFailure(BarikoiTraceErrors.serverError());

                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String,String> header= new HashMap<>();
                header.put("Content-Type","application/json");
                return header;
            }
            @Override
            public byte[] getBody() throws AuthFailureError {
                HashMap<String,Object> params=new HashMap<>();
                params.put("api_key",key);
                params.put("user_id",id);

                params.put("gpx_bulk", data);


                return new JSONObject(params).toString().getBytes();
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(120 * 1000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        request.setShouldCache(false);
        requestQueue.add(request);
    }

    public void startTrip(final String startTime,  final TraceMode tracemode, final String tag, final BarikoiTraceTripApiCallback callback ){


        StringRequest request = new StringRequest(Request.Method.POST,
                Api.start_trip_url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject responsejson=new JSONObject(response);
                            Log.d("tripstart", responsejson.toString());
                            String status= responsejson.getString("status");
                            if(status.equals("success")){
                                Trip trip = JsonResponseAdapter.getTrip(responsejson.getJSONObject("trip"));
                                callback.onSuccess(trip);
                            }else {
                                String msg= responsejson.getString("error");
                                callback.onFailure(new BarikoiTraceError(status,msg));
                            }
                        } catch (JSONException e) {
                            callback.onFailure(BarikoiTraceErrors.jsonResponseError(e));
                        }
                    }

                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
//                        Log.d("BarikoiTraceTrip","error:"+error.networkResponse.statusCode);
                        if (error instanceof NetworkError || error instanceof NoConnectionError || error instanceof  TimeoutError)
                            callback.onFailure(BarikoiTraceErrors.networkError());
                        else if (error.networkResponse!=null && error.networkResponse.data!=null){
                            String errorstring = new String(error.networkResponse.data);
                            try {
                                JSONObject responsejson=new JSONObject(errorstring);
                                String msg= responsejson.getString("error");
                                callback.onFailure(new BarikoiTraceError(error.networkResponse.statusCode+"",msg));
                            } catch (JSONException e) {
                                callback.onFailure(BarikoiTraceErrors.serverError());
                            }
                        }else callback.onFailure(BarikoiTraceErrors.serverError());
                    }
                }
        ) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                HashMap<String,Object> params=new HashMap<>();
                params.put("api_key",key);
                params.put("user_id",id);
                params.put("start_time",startTime);
                if(tag!=null)
                    params.put("tag",tag);
                if(tracemode.isInDebugMode())
                    params.put("debug", tracemode.isInDebugMode());
                return new JSONObject(params).toString().getBytes();
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String,String> header= new HashMap<>();
                header.put("Content-Type","application/json");
                return header;
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(240 * 1000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        request.setShouldCache(false);
        requestQueue.add(request);
    }

    public void syncOfflineTrip(final Trip trip ,final BarikoiTraceTripApiCallback callback){
        HashMap<String,String> params=new HashMap<>();
        params.put("api_key",key);
        params.put("user_id",id);
        params.put("start_time",trip.getStart_time());
        params.put("end_time",trip.getEnd_time());
        if(trip.getTag()!=null)
            params.put("tag",trip.getTag());
        params.put("state",trip.getState()+"");


        StringRequest request = new StringRequest(Request.Method.POST,
                Api.trip_sync_url+paramString(params),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject responsejson=new JSONObject(response);
                            int status= responsejson.getInt("status");
                            if(status==200 || status ==201){

                                callback.onSuccess(trip);

                            }else {
                                String msg= responsejson.getString("message");
                                callback.onFailure(new BarikoiTraceError(status+"",msg));
                            }
                        } catch (JSONException e) {
                            callback.onFailure(BarikoiTraceErrors.jsonResponseError(e));
                        }
                    }

                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("BarikoiTraceTrip","error:"+error.getMessage());
                        //loading.setVisibility(View.GONE);
                        //Toast.makeText(context, "problem", Toast.LENGTH_SHORT).show();
                        //NetworkcallUtils.handleResponse(error,context);
                        callback.onFailure( BarikoiTraceErrors.serverError());
                    }
                }
        ) {
            /*@Override
            protected Map<String, String> getParams() throws AuthFailureError {

                return params;
            }*/
        };

        requestQueue.add(request);
    }

    public void endTrip(final String endTime, final BarikoiTraceTripApiCallback callback ){
        StringRequest request = new StringRequest(Request.Method.POST,
                Api.end_trip_url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject responsejson=new JSONObject(response);
                            String status= responsejson.getString("status");
                            Log.d("tripend", responsejson.toString());
                            if(status.equals("success")){
                                callback.onSuccess(JsonResponseAdapter.getTrip(responsejson.getJSONObject("trip")));
                            }else {
                                String msg= responsejson.getString("message");
                                callback.onFailure(new BarikoiTraceError(status,msg));
                            }
                        } catch (JSONException e) {
                            callback.onFailure(BarikoiTraceErrors.jsonResponseError(e));
                        }
                    }

                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error!=null )Log.d("BarikoiTraceTrip","error:"+error.getMessage());
                        if (error instanceof NetworkError || error instanceof NoConnectionError || error instanceof  TimeoutError)
                            callback.onFailure(BarikoiTraceErrors.networkError());
                        else if (error.networkResponse!=null && error.networkResponse.data!=null){
                            String errorstring = new String(error.networkResponse.data);
                            try {
                                JSONObject responsejson=new JSONObject(errorstring);
                                String msg= responsejson.getString("error");
                                callback.onFailure(new BarikoiTraceError(error.networkResponse.statusCode+"",msg));
                            } catch (JSONException e) {
                                callback.onFailure(BarikoiTraceErrors.serverError());
                            }
                        }else callback.onFailure(BarikoiTraceErrors.serverError());
                    }
                }
        ) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                HashMap<String, Object> params= new HashMap<>();
                params.put("api_key",key);
                params.put("user_id",id);
                params.put("end_time",endTime);
                return new JSONObject(params).toString().getBytes();
            }
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String,String> header= new HashMap<>();
                header.put("Content-Type","application/json");
                return header;
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(240 * 1000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        request.setShouldCache(false);
        requestQueue.add(request);
    }
    public void getCurrentTrip( final BarikoiTraceGetTripCallback callback ){
        HashMap<String,String> params=new HashMap<>();
        params.put("api_key",key);
        params.put("user_id",id);

        StringRequest request = new StringRequest(Request.Method.GET,
                Api.active_trip_url+paramString(params),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject responsejson=new JSONObject(response);
                            boolean active= responsejson.getBoolean("active");
                            if(active){
                                Trip trip =JsonResponseAdapter.getTrip(responsejson.getJSONObject("trip"));
                                callback.onSuccess(trip);
                            }else {
                                callback.onSuccess(null);
                            }
                        } catch (JSONException e) {
                            callback.onFailure(BarikoiTraceErrors.jsonResponseError(e));
                        }
                    }

                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error!=null )Log.d("BarikoiTraceTrip","error:"+error.getMessage());
                        if (error instanceof NetworkError || error instanceof NoConnectionError || error instanceof  TimeoutError)
                            callback.onFailure(BarikoiTraceErrors.networkError());
                        else callback.onFailure(BarikoiTraceErrors.serverError());
                    }
                }
        ) {

        };
        request.setRetryPolicy(new DefaultRetryPolicy(120 * 1000, 1,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        request.setShouldCache(false);
        requestQueue.add(request);
    }



    public void syncSettings(final BarikoiTraceSettingsCallback callback){
        HashMap<String,String> params=new HashMap<>();
        params.put("api_key",key);

        StringRequest request = new StringRequest(Request.Method.GET,
                Api.company_settings+paramString(params),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject responsejson=new JSONObject(response);
                            int status= responsejson.getInt("status");
                            if(status==200 ){
                                TraceMode mode=JsonResponseAdapter.getCompanySettings(responsejson.getJSONObject("settings"));
                                configStorageManager.setTraceMode(mode);
                                callback.onSuccess(mode);

                            }else {
                                String msg= responsejson.getString("message");
                                callback.onFailure(new BarikoiTraceError(status+"",msg));
                            }
                        } catch (JSONException e) {
                            if(configStorageManager.getTraceMode()==null)
                                callback.onFailure(BarikoiTraceErrors.jsonResponseError(e));
                        }
                    }

                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error!=null )Log.d("BarikoiTraceTrip","error:"+error.getMessage());
                        //loading.setVisibility(View.GONE);
                        //Toast.makeText(context, "problem", Toast.LENGTH_SHORT).show();
                        //NetworkcallUtils.handleResponse(error,context);
                        callback.onFailure( BarikoiTraceErrors.serverError());
                    }
                }
        ) {

        };
        request.setRetryPolicy(new DefaultRetryPolicy(240 * 1000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        request.setShouldCache(false);
        requestQueue.add(request);
    }



    public void insertLogFile(final String path, final BarikoiTraceBulkUpdateCallback callback) {
        VolleyMultipartRequest request = new VolleyMultipartRequest(Request.Method.POST, Api.app_log_url,
                new Response.Listener<NetworkResponse>() {
                    @Override
                    public void onResponse(NetworkResponse response) {
                        if (response.statusCode == 200){
                            callback.onBulkUpdate();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        callback.onFailure(BarikoiTraceErrors.networkError());
                    }
                }){
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> parameters = new HashMap<>();
                parameters.put("user_id", id);
                return parameters;
            }

            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            protected Map<String, DataPart> getByteData() throws AuthFailureError {
                Map<String, DataPart> parameters = new HashMap<>();
                String filename = path.substring(path.lastIndexOf("/"));
                parameters.put("log", new DataPart(filename, getFileData(path)));
                return parameters;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(240 * 1000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(request);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static byte[] getFileData(String f) {
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
            e.printStackTrace();
        }
        return bytes;
    }
    private void setId(String id){
        INSTANCE.id=id;

    }

    public void setKey(String key){
        INSTANCE.key=key;
    }




    private String paramString(HashMap<String,String> params){
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (first) {
                first = false;
                result.append("?");
            }
            else
                result.append("&");

            try {
                result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                // This exception should never occur for UTF-8 encoding
                throw new RuntimeException("Failed to encode parameters", e);
            }
        }

        return result.toString();
    }





}
