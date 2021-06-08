package com.barikoi.barikoitrace.network;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.barikoi.barikoitrace.Utils.DateTimeUtils;
import com.barikoi.barikoitrace.callback.BarikoiTraceBulkUpdateCallback;
import com.barikoi.barikoitrace.callback.BarikoiTraceGetTripCallback;
import com.barikoi.barikoitrace.callback.BarikoiTraceLocationUpdateCallback;
import com.barikoi.barikoitrace.callback.BarikoiTraceTripApiCallback;
import com.barikoi.barikoitrace.callback.BarikoiTraceUserCallback;
import com.barikoi.barikoitrace.exceptions.BarikoiTraceException;
import com.barikoi.barikoitrace.localstorage.ConfigStorageManager;
import com.barikoi.barikoitrace.models.BarikoiTraceError;
import com.barikoi.barikoitrace.models.BarikoiTraceErrors;
import com.barikoi.barikoitrace.models.BarikoiTraceUser;
import com.barikoi.barikoitrace.models.createtrip.Trip;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public class ApiRequestManager {
    private static ApiRequestManager INSTANCE;
    private ConfigStorageManager configStorageManager;
    private RequestQueue requestQueue;
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
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject responsejson=new JSONObject(response);
                            int status= responsejson.getInt("status");
                            if(status==200){
                                JSONObject userjson=responsejson.getJSONObject("user");
                                int id= userjson.getInt("id");
                                String name= userjson.getString("name");
                                String email=userjson.getString("email");
                                String phone=userjson.getString("phone");
                                BarikoiTraceUser user=new BarikoiTraceUser(id+"", email,phone);
                                configStorageManager.setUserID(user.getUserId());
                                setId(id+"");
                                callback.onSuccess(user);
                            }else {
                                String msg= responsejson.getString("message");
                                callback.onFailure(new BarikoiTraceError(status+"",msg));
                            }
                        } catch (JSONException e) {
                            callback.onFailure(BarikoiTraceErrors.jsonResponseError());
                        }
                    }

                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("locationupdate","error:"+error.getMessage());
                        callback.onFailure(BarikoiTraceErrors.serverError());
                        //loading.setVisibility(View.GONE);
                        //Toast.makeText(context, "problem", Toast.LENGTH_SHORT).show();
                        //NetworkcallUtils.handleResponse(error,context);
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String,String> params=new HashMap<>();
                params.put("api_key",key);
                if(!TextUtils.isEmpty(email)) params.put("email",email);
                if(!TextUtils.isEmpty(phone)) params.put("phone",phone);
                return params;
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
                            callback.onFailure(BarikoiTraceErrors.jsonResponseError());
                        }
                    }

                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("locationupdate","error:"+error.getMessage());
                        //loading.setVisibility(View.GONE);
                        //Toast.makeText(context, "problem", Toast.LENGTH_SHORT).show();
                        //NetworkcallUtils.handleResponse(error,context);
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String,String> params=new HashMap<>();
                params.put("api_key",key);
                params.put("user_id",id);

                params.put("latitude",latitude+"");
                params.put("longitude",longitude+"");
                params.put("altitude",altitude+"");
                params.put("speed",speed+"");
                params.put("bearing",bearing+"");
                params.put("gpx_time",timestring);


                 return params;
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(40 * 1000, 0,
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
                            callback.onFailure(BarikoiTraceErrors.jsonResponseError());
                        }
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
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String,String> params=new HashMap<>();
                params.put("api_key",key);
                params.put("user_id",id);

                params.put("gpx_bulk", data.toString());


                return params;
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(40 * 1000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        request.setShouldCache(false);
        requestQueue.add(request);
    }

    public void startTrip(final String startTime, final String tag, final BarikoiTraceTripApiCallback callback ){
        HashMap<String,String> params=new HashMap<>();
        params.put("api_key",key);
        params.put("user_id",id);
        params.put("start_time",startTime);
        if(tag!=null)
            params.put("tag",tag);

        StringRequest request = new StringRequest(Request.Method.POST,
                Api.start_trip_url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject responsejson=new JSONObject(response);
                            int status= responsejson.getInt("status");
                            if(status==200 || status ==201){

                                callback.onSuccess();
                            }else {
                                String msg= responsejson.getString("message");
                                callback.onFailure(new BarikoiTraceError(status+"",msg));
                            }
                        } catch (JSONException e) {
                            callback.onFailure(BarikoiTraceErrors.serverError());
                        }
                    }

                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("BarikoiTraceTrip","error:"+error.getMessage());
                        callback.onFailure(BarikoiTraceErrors.serverError());
                        //loading.setVisibility(View.GONE);
                        //Toast.makeText(context, "problem", Toast.LENGTH_SHORT).show();
                        //NetworkcallUtils.handleResponse(error,context);
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String,String> params=new HashMap<>();
                params.put("api_key",key);
                params.put("user_id",id);
                params.put("start_time",startTime);
                if(tag!=null)
                    params.put("tag",tag);
                return params;
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(40 * 1000, 0,
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

                                callback.onSuccess();

                            }else {
                                String msg= responsejson.getString("message");
                                callback.onFailure(new BarikoiTraceError(status+"",msg));
                            }
                        } catch (JSONException e) {
                            callback.onFailure(BarikoiTraceErrors.jsonResponseError());
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
        HashMap<String,String> params=new HashMap<>();
        params.put("api_key",key);
        params.put("user_id",id);
        params.put("end_time",endTime);

        StringRequest request = new StringRequest(Request.Method.POST,
                Api.end_trip_url+paramString(params),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject responsejson=new JSONObject(response);
                            int status= responsejson.getInt("status");
                            if(status==200 || status ==201){

                                callback.onSuccess();
                            }else {
                                String msg= responsejson.getString("message");
                                callback.onFailure(new BarikoiTraceError(status+"",msg));
                            }
                        } catch (JSONException e) {
                            callback.onFailure(BarikoiTraceErrors.jsonResponseError());
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
           /* @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String,String> params=new HashMap<>();
                params.put("api_key",key);
                params.put("user_id",id);
                params.put("end_time",endTime);
                return params;
            }*/
        };

        requestQueue.add(request);
    }
    public void getCurrentTrips( final BarikoiTraceGetTripCallback callback ){
        HashMap<String,String> params=new HashMap<>();
        params.put("api_key",key);
        params.put("user_id",id);

        StringRequest request = new StringRequest(Request.Method.GET,
                Api.activ_trip_url+paramString(params),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject responsejson=new JSONObject(response);
                            int status= responsejson.getInt("status");
                            if(status==200 || status ==201){
                                callback.onSuccess(JsonResponseAdapter.getTrips(responsejson.getJSONObject("data").getJSONArray("trips")));
                            }else {
                                String msg= responsejson.getString("message");
                                callback.onFailure(new BarikoiTraceError(status+"",msg));
                            }
                        } catch (JSONException e) {
                            callback.onFailure(BarikoiTraceErrors.jsonResponseError());
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

        requestQueue.add(request);
    }


    private void setId(String id){
        INSTANCE.id=id;

    }

    public void setKey(String key){
        INSTANCE.key=key;
    }




    private String paramString(HashMap<String,String> map){

        String params="?";
        for (Map.Entry<String, String> mapElement : map.entrySet()) {

            try {
                params=params+(mapElement.getKey() + "=" + URLEncoder.encode(String.valueOf(mapElement.getValue()), "UTF-8" )+"&");

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return params.substring(0,params.length()-1);

    }




}
