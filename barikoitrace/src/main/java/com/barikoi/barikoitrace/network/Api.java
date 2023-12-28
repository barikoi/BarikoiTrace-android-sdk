package com.barikoi.barikoitrace.network;


public class Api {
    public static final String base_url="https://tracev2.barikoimaps.dev/sdk";
    public static final String base_url_2="https://trace.barikoi.xyz/api/v1/";

    public static final String start_trip_url=base_url+"/start_trip";
    public static final String end_trip_url=base_url+"/end_trip";
    public static final String trip_sync_url=base_url+"/trip/offline";
    public static final String gpx_url= base_url+"/add-gpx";
    public static final String bulk_url= base_url+"/bulk-gpx";
    public static final String user_url=base_url+"/user";
    public static final String get_create_user_url=base_url+"/authenticate";
    public static final String NAME="name",PHONE="phone",EMAIL="email",TOKEN="token", PATH="file_path", URI_PATH="uri_path",USER_ID="user_id";
    public static final String active_trip_url=base_url+"/user/active_trip";
    public static final String company_settings=base_url+"/get/company/settings";
    public static final String app_log_url=base_url+"app/log";
}
