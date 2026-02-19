package com.barikoi.barikoitrace.network;


public class Api {
    // SECURITY FIX: Use HTTPS instead of HTTP for secure API communication
    // TODO: Ensure server is configured for HTTPS before deployment
    public static String base_url="https://api.mqtt.bmapsbd.com/api/v1/";
    // SECURITY FIX: Use SSL/TLS instead of plain TCP for secure MQTT connection
    // TODO: Ensure MQTT broker is configured for SSL (typically port 8883) before deployment
    public static String mqtt_url="ssl://mqtt.bmapsbd.com:8883";
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
