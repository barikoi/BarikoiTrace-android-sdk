# ==========================================
# BarikoiTrace SDK - Consumer ProGuard Rules
# ==========================================

# --- Public API ---

-keep class com.barikoi.barikoitrace.BarikoiTrace { *; }
-keep class com.barikoi.barikoitrace.BarikoiTrace$* { *; }

# --- Models ---

-keep class com.barikoi.barikoitrace.model.TraceUser { *; }
-keep class com.barikoi.barikoitrace.model.TraceError { *; }
-keep class com.barikoi.barikoitrace.model.TraceError$* { *; }
-keep class com.barikoi.barikoitrace.TraceMode { *; }
-keep class com.barikoi.barikoitrace.TraceMode$* { *; }
-keep class com.barikoi.barikoitrace.TraceMode$Builder { *; }

# --- Services & Receivers (instantiated by Android via reflection) ---

-keep class com.barikoi.barikoitrace.service.LocTraceForegroundService { *; }
-keep class com.barikoi.barikoitrace.service.LocTraceDataService { *; }
-keep class com.barikoi.barikoitrace.receiver.BootReceiver { *; }
-keep class com.barikoi.barikoitrace.receiver.LocationReceiver { *; }
-keep class com.barikoi.barikoitrace.receiver.LocationReceiver$EventCallback { *; }

# --- Listeners & Callbacks ---

-keep interface com.barikoi.barikoitrace.location.LocationUpdateListener { *; }

# --- Room ---

-keep class com.barikoi.barikoitrace.storage.OfflineLocationEntity { *; }
-keep class com.barikoi.barikoitrace.storage.OfflineLocationDao { *; }
-keep class com.barikoi.barikoitrace.storage.OfflineLocationDb { *; }
-keep class com.barikoi.barikoitrace.storage.OfflineLocationDb$* { *; }

# --- Retrofit ---

-keep interface com.barikoi.barikoitrace.api.TraceApiService { *; }
-keep class com.barikoi.barikoitrace.api.TraceApiClient { *; }
-keep class com.barikoi.barikoitrace.api.ApiRoutes { *; }
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
