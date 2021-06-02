package com.barikoi.barikoitrace.network;

import android.location.Location;

import com.barikoi.barikoitrace.Utils.DateTimeUtils;
import com.barikoi.barikoitrace.exceptions.BarikoiTraceException;
import com.barikoi.barikoitrace.models.BarikoiTraceUser;
import com.barikoi.barikoitrace.models.createtrip.Trip;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;

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

    public static ArrayList<Trip> getTrips(JSONArray array){
        ArrayList<Trip> trips = new ArrayList<>();
        try{
            for(int i = 0; i <array.length(); i++){
                JSONObject tripjson= array.getJSONObject(i).getJSONObject("trip_info");
                int id= tripjson.has("id")?tripjson.getInt("id"): i;
                String startTime= tripjson.getString("start_time");
                int state = tripjson.getInt("state");
                int user_id=tripjson.has("id")?tripjson.getInt("user_id"):0;
                String tag = tripjson.getString("tag");
                String endTime =null;
                if(tripjson.has("start_time")) endTime= tripjson.getString("start_time");
                trips.add(new Trip(id,startTime,endTime,tag,state,user_id,1));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return trips;
    }


}
