package com.barikoi.barikoitrace.network;


public class Api {
    public String base_url="http://api.mqtt.bmapsbd.com/api/v1";
    public String mqtt_url="tcp://mqtt.bmapsbd.com:1883";
    public String start_trip_url=base_url+"/trip/create";
    public String end_trip_url=base_url+"/trip/end";
    public String trip_sync_url=base_url+"/trip/offline";
    public String gpx_url= base_url+"/sdk/add-gpx";
    public String bulk_url= base_url+"/sdk/bulk-gpx";
    public String user_url=base_url+"/sdk/user";
    public String get_create_user_url=base_url+"/sdk/authenticate";
    public String active_trip_url=base_url+"/trip/check-active-trip";
    public String company_settings=base_url+"/sdk/company/settings";
    public String app_log_url=base_url+"/app/log";

    public static Api INSTANCE;
    
   public static Api getInstance(){
       if (INSTANCE!= null ) return INSTANCE;
       INSTANCE=new Api();
       return INSTANCE;
   }

   public Api(){

   }

   public void setBaseURL(String url){
       base_url=url;
   }

   public void setMqtt_url(String url){
       mqtt_url=url;
   }
}
