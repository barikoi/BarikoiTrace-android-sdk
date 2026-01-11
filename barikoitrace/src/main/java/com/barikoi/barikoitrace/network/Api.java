package com.barikoi.barikoitrace.network;


public class Api {
    public static String base_url="http://api.mqtt.bmapsbd.com/api/v1";
    public static String mqtt_url="tcp://mqtt.bmapsbd.com:1883";
    public static String start_trip_url="/trip/create";
    public static String end_trip_url="/trip/end";
    public static String trip_sync_url="/trip/offline";
    public static String gpx_url= "/sdk/add-gpx";
    public static String bulk_url= "/sdk/bulk-gpx";
    public static String user_url="/sdk/user";
    public static String get_create_user_url="/sdk/authenticate";
    public static String active_trip_url="/trip/check-active-trip";
    public static String company_settings="/sdk/company/settings";
    public static String app_log_url="/app/log";


}
