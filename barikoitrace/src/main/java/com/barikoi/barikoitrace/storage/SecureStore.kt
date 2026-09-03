package com.barikoi.barikoitrace.storage

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Keystore-backed storage for credentials and user PII.
 *
 * Counterpart to the iOS SDK's `KeychainStore`. `TraceDataStore` keeps the
 * split the iOS side uses: secrets and identity here, runtime config and
 * feature flags in plain DataStore Preferences. Previously the API key, the
 * broker username/password and every user field sat in the same world-readable
 * (on a rooted device) preferences file as the tracking mode.
 *
 * Falls back to plain `SharedPreferences` if the Keystore is unavailable —
 * some devices genuinely fail `MasterKey` creation, and a tracking SDK that
 * refuses to start at all is worse than one that stores the same data the way
 * it used to. The fallback is logged, not silent.
 */
internal class SecureStore(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.w(TAG, "Keystore unavailable, falling back to plain preferences", e)
        context.getSharedPreferences(FALLBACK_FILE_NAME, Context.MODE_PRIVATE)
    }

    fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getString(key: String): String? = prefs.getString(key, null)

    fun putLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    fun getLong(key: String, default: Long = 0L): Long = prefs.getLong(key, default)

    fun remove(vararg keys: String) {
        prefs.edit().apply { keys.forEach { remove(it) } }.apply()
    }

    companion object {
        private const val TAG = "SecureStore"
        private const val FILE_NAME = "barikoi_trace_secure"
        private const val FALLBACK_FILE_NAME = "barikoi_trace_secure_plain"
    }
}
