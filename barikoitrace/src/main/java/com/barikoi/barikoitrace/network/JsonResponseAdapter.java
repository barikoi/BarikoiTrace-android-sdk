package com.barikoi.barikoitrace.network;

import com.barikoi.barikoitrace.exceptions.BarikoiTraceException;
import com.barikoi.barikoitrace.models.BarikoiTraceErrors;
import com.barikoi.barikoitrace.models.BarikoiTraceUser;

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
}
