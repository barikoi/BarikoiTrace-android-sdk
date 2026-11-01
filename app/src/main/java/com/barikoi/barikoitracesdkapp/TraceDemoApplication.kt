package com.barikoi.barikoitracesdkapp

import android.app.Application
import android.content.Context
import com.barikoi.barikoitrace.BarikoiTrace

class TraceDemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Activity recreation must not replace a key entered during this session.
        BarikoiTrace.initialize(this, DemoPreferences(this).apiKey)
    }
}

internal class DemoPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("trace_demo", Context.MODE_PRIVATE)
    var apiKey: String
        get() = preferences.getString("api_key_override", null) ?: BuildConfig.API_KEY
        set(value) { preferences.edit().putString("api_key_override", value).apply() }
}
