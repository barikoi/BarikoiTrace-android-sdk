# ==========================================
# BarikoiLocTrace SDK - Consumer ProGuard Rules
# ==========================================

# --- Public API ---

-keep class com.barikoi.barikoiloctrace.BarikoiLocTrace { *; }
-keep class com.barikoi.barikoiloctrace.BarikoiLocTrace$* { *; }

# --- Models ---

-keep class com.barikoi.barikoiloctrace.model.TraceUser { *; }
-keep class com.barikoi.barikoiloctrace.model.TraceError { *; }
-keep class com.barikoi.barikoiloctrace.model.TraceError$* { *; }
-keep class com.barikoi.barikoiloctrace.TraceMode { *; }
-keep class com.barikoi.barikoiloctrace.TraceMode$* { *; }
-keep class com.barikoi.barikoiloctrace.TraceMode$Builder { *; }

# --- Services & Receivers (instantiated by Android via reflection) ---

-keep class com.barikoi.barikoiloctrace.service.LocTraceForegroundService { *; }
-keep class com.barikoi.barikoiloctrace.service.LocTraceDataService { *; }
-keep class com.barikoi.barikoiloctrace.receiver.BootReceiver { *; }
-keep class com.barikoi.barikoiloctrace.receiver.LocationReceiver { *; }
-keep class com.barikoi.barikoiloctrace.receiver.LocationReceiver$EventCallback { *; }

# --- Listeners & Callbacks ---

-keep interface com.barikoi.barikoiloctrace.location.LocationUpdateListener { *; }

# --- Room ---

-keep class com.barikoi.barikoiloctrace.storage.OfflineLocationEntity { *; }
-keep class com.barikoi.barikoiloctrace.storage.OfflineLocationDao { *; }
-keep class com.barikoi.barikoiloctrace.storage.OfflineLocationDb { *; }
-keep class com.barikoi.barikoiloctrace.storage.OfflineLocationDb$* { *; }

# --- Retrofit ---

-keep interface com.barikoi.barikoiloctrace.api.TraceApiService { *; }
-keep class com.barikoi.barikoiloctrace.api.TraceApiClient { *; }
-keep class com.barikoi.barikoiloctrace.api.ApiRoutes { *; }
-keepclassmembers,allowobfuscation class * {
  @retrofit2.http.* <methods>;
}

# --- MQTT (Paho Android) ---

-keep class org.eclipse.paho.client.mqttv3.** { *; }
-keep class info.mqtt.android.service.** { *; }
-dontwarn org.eclipse.paho.client.mqttv3.**
-dontwarn info.mqtt.android.service.**

# --- Gson serialization (keep field names for JSON parsing) ---

-keepattributes Signaturewhat
