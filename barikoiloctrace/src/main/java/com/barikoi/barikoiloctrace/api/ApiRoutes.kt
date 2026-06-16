package com.barikoi.barikoiloctrace.api

object ApiRoutes {
    const val BASE_URL = "https://api.mqtt.bmapsbd.com/api/v1/"
    const val MQTT_URL = "tcp://mqtt.bmapsbd.com:1883"

    const val AUTHENTICATE = "/sdk/authenticate"
    const val ADD_GPX = "/sdk/add-gpx"
    const val BULK_GPX = "/sdk/bulk-gpx"
    const val USER = "/sdk/user"
    const val START_TRIP = "/trip/create"
    const val END_TRIP = "/trip/end"
    const val TRIP_SYNC = "/trip/offline"
    const val ACTIVE_TRIP = "/trip/check-active-trip"
    const val COMPANY_SETTINGS = "/sdk/company/settings"
    const val APP_LOG = "/app/log"
}
