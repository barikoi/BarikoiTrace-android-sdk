package com.barikoi.barikoitrace.network;

import android.location.Location;
import android.util.Log;

import com.barikoi.barikoitrace.TraceMode;
import com.barikoi.barikoitrace.utils.DateTimeUtils;
import com.barikoi.barikoitrace.exceptions.BarikoiTraceException;
import com.barikoi.barikoitrace.models.BarikoiTraceUser;
import com.barikoi.barikoitrace.models.createtrip.Trip;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class JsonResponseAdapter {
    static BarikoiTraceUser getUser(String response) throws BarikoiTraceException{
        try {
            JSONObject responsejson=new JSONObject(response);
            int status= responsejson.getInt("status");
            if(status==200){
                JSONObject userjson=responsejson.getJSONObject("user");
                String id= userjson.getString("_id");
                String name= userjson.getString("name");
                String email=userjson.getString("email");
                String phone=userjson.getString("phone");
                return new BarikoiTraceUser.Builder()
                        .setUserId(id)
                        .setName(name)
                        .setEmail(email)
                        .setPhone(phone)
                        .build();
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
            params.put("accuracy",location.getAccuracy());
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
                String id= tripjson.has("id")?tripjson.getString("id"): "";
                String startTime= tripjson.getString("start_time");
                int state = tripjson.getInt("state");
                String user_id=tripjson.has("id")?tripjson.getString("user_id"):"";
                String tag = tripjson.getString("tag");
                String endTime =null;
                if(tripjson.has("start_time")) endTime= tripjson.getString("start_time");
                trips.add(new Trip(id,startTime,endTime,tag,state,user_id,1));
            }
        } catch (JSONException e) {
            Log.e("Tracejson",e.getMessage());
        }
        return trips;
    }


    public static Trip getTrip(JSONObject tripjson) throws JSONException{
        String id= tripjson.has("_id")?tripjson.getString("_id"): "";
        String startTime= tripjson.getString("start_time");
        int state = tripjson.getInt("status");
        String user_id=tripjson.has("user")?tripjson.getString("user"):"";
        String tag = tripjson.getString("tag");
        String endTime =null;
        if(tripjson.has("end_time")) endTime= tripjson.getString("end_time");
        return new Trip(id,startTime,endTime,tag,state,user_id,1);
    }
    public static TraceMode getCompanySettings(JSONObject obj) throws JSONException{

        return new TraceMode.Builder().setUpdateInterval(obj.getInt("update_time_interval"))
                .setDistancefilter(obj.getInt("distance_interval"))
                .setAccuracyFilter(obj.getInt("accuracy_filter"))
                .setOfflineSync(obj.getInt("offline_sync") == 1)
                .build();

    }


}
