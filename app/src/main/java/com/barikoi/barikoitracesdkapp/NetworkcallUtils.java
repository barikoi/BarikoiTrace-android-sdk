package com.barikoi.barikoitracesdkapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Sakib on 1/15/2018.
 */

public final class NetworkcallUtils {


    public static void logout(final Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        final String token=prefs.getString(Api.TOKEN,"");
        SharedPreferences.Editor editor=prefs.edit();

        editor.remove(Api.TOKEN);
        editor.remove(Api.ENROLLED);
        editor.remove(Api.NAME);
        editor.remove(Api.USER_ID);
        editor.remove(Api.PHONE);
        editor.remove(Api.POINTS);

        editor.commit();
        RequestQueue queue= RequestQueueSingleton.getInstance(context.getApplicationContext()).getRequestQueue();
        StringRequest request = new StringRequest(Request.Method.DELETE,
                Api.logouturl,
                response -> {
                    Intent home = new Intent(context, LoginActivity.class);
                    home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    context.startActivity(home);

//            if (context.getClass() != LoginActivity.class) {
//                Intent home = new Intent(context, LoginActivity.class);
//                home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                context.startActivity(home);
//            }else{
//                Intent intent = new Intent();
//                context.startActivity(intent);
//            }
                },
                error -> {
                    Intent home =new Intent(context,LoginActivity.class);
                    home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    context.startActivity(home);

                }) {
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<String, String>();
                if(!token.equals("")){
                    params.put("Authorization", "bearer "+token);
                }
                return params;
            }
        };
        queue.add(request);
    }
    public static HashMap<String,String> splitAddress(String placeAddress,String ptype){
        HashMap<String,String> d=new HashMap<String, String>();
        String[]addresscomponents=placeAddress.split(",");

        for (int i=0;i<addresscomponents.length;i++){
            String comp=addresscomponents[i].trim();
            if (i == 0){
                if (!ishouse(comp) && !isroad(comp)){
                    if(!ptype.contains("Residential") && !ptype.contains("Admin"))
                        d.put("business_name",comp);
                    else if(!ishouse(addresscomponents[1]) && !isroad(addresscomponents[1]) && !isFloor(addresscomponents[1])) d.put("business_name",comp);
                    else d.put("name",comp);
                }
                else if (ishouse(comp))
                    d.put("holding_no", comp);
                else if (isroad(comp))
                    d.put("road_name_number",comp);
            }
           else{
                if(i==1 && !ishouse(comp) && !isroad(comp) && !isFloor(comp) && d.containsKey("business_name")){
                    d.put("name",comp);
                    continue;
                }
                if(isFloor(comp))
                    d.put("floor",comp);
                else if (ishouse(comp) && !d.containsKey("holding_no"))
                    d.put("holding_no", comp);

                else if (isroad(comp))
                    d.put("road_name_number",comp);

                else if (addresscomponents.length > i + 1)
                    if(d.get("super_sub_area")!=null)
                        d.put("super_sub_area",d.get("super_sub_area")+", "+comp);
                    else
                        d.put("super_sub_area",comp);
                else
                    d.put("sub_area",comp);

            }
        }
        Log.d("addressparse",d.toString());
        return d;
    }

    public static boolean isroad(String road){
        road=road.toLowerCase();

        return road.contains("road") || road.contains("ave") || road.contains("lane") || road.contains("sarani") || road.contains("soroni") || road.contains("shoroni")|| road.contains("sharani") ;
    }
    public static boolean ishouse(String house){
        house=house.toLowerCase();

        return house.contains("house") && house.matches(".*\\d.*");
    }
    public static boolean isFloor(String house){
        house=house.toLowerCase();
        return (house.contains("flat") || house.contains("floor"));
    }

    public static void deleteFileLocal(String filepath){
        if(filepath != null) {
            new File(filepath).delete();
        }
    }


    public static void clearInputForm(LinearLayout layout){

        for (int i = 0; i<layout.getChildCount(); i++){
            View view = layout.getChildAt(i);
        }


    }

}
