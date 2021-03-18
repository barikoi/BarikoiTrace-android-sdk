package com.barikoi.barikoitrace.network;

import android.location.Location;

import com.barikoi.barikoitrace.Utils.DateTimeUtils;
import com.barikoi.barikoitrace.exceptions.BarikoiTraceException;
import com.barikoi.barikoitrace.models.BarikoiTraceUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonResponseAdapter {
    static BarikoiTraceUser getUser(String response) throws BarikoiTraceException{
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
                return user;
            }else {
                return null;
            }
        } catch (JSONException e) {
            throw new BarikoiTraceException(e);
        }

    }

    public static JSONObject getlocationJson(Location location) throws BarikoiTraceException {
        JSONObject params=params = new JSONObject();
        try {
            params.put("latitude",location.getLatitude());
            params.put("longitude", location.getLongitude());
            params.put("bearing",location.getBearing());
            params.put("altitude", location.getAltitude());
            params.put("gpx_time", DateTimeUtils.getDateTimeLocal(location.getTime()));
            params.put("speed",location.getSpeed());
        }catch(JSONException e){
            throw new BarikoiTraceException(e);
        }
        return params;
    }



}
