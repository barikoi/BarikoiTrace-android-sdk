package com.barikoi.barikoitrace.network;


public class Api {
    public static final String base_url="https://barikoi.xyz/v1/api/trace";
    public static final String start_trip_url=base_url+"/trip/start";
    public static final String end_trip_url=base_url+"/trip/end";
    public static final String trip_sync_url=base_url+"/trip/offline";
    public static final String gpx_url= base_url+"/position/push";
    public static final String bulk_url= base_url+"/position/bulk/push";
    public static final String user_url=base_url+"/user";
    public static final String NAME="name",PHONE="phone",EMAIL="email",TOKEN="token", PATH="file_path", URI_PATH="uri_path",USER_ID="user_id",POINTS="points",SPENT_POINTS="redeemed_points",ISREFFERED="isReferred",REFCODE="ref_code";


    public static final String activ_trip_url=base_url+"/user/active/trip";
}
