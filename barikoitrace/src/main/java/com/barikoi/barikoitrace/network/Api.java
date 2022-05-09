package com.barikoi.barikoitrace.network;


public class Api {
    public static final String base_url=" https://backend.barikoi.com:8888/api/v1/business";
    public static final String base_url_2=" https://backend.barikoi.com:8888/api/v1/";

    public static final String start_trip_url=base_url+"/start_trip";
    public static final String end_trip_url=base_url+"/end_trip";
    public static final String trip_sync_url=base_url+"/trip/offline";
    public static final String gpx_url= base_url+"/gpx";
    public static final String bulk_url= base_url+"/bulk/gpx";
    public static final String user_url=base_url+"/user";
    public static final String get_create_user_url=base_url+"/authenticate";
    public static final String NAME="name",PHONE="phone",EMAIL="email",TOKEN="token", PATH="file_path", URI_PATH="uri_path",USER_ID="user_id",POINTS="points",SPENT_POINTS="redeemed_points",ISREFFERED="isReferred",REFCODE="ref_code";
    public static final String activ_trip_url=base_url+"/user/active_trip";
    public static final String company_settings=base_url+"/get/company/settings";
    public static final String app_log_url=base_url_2+"app/log";
}
