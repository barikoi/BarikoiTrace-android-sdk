package com.barikoi.barikoitrace.util

import android.content.Context
import android.util.Log
import com.barikoi.barikoitrace.BarikoiTrace
import com.barikoi.barikoitrace.storage.TraceDataStore

/** SDK diagnostics only: callers must omit payloads, identifiers, and credentials. */
internal class TraceLogger(context: Context, private val tag: String) {
    private val dataStore = TraceDataStore(context)

    fun d(message: String) = log(Log.DEBUG, "DEBUG", message)
    fun i(message: String) = log(Log.INFO, "INFO", message)
    fun w(message: String, error: Throwable? = null) = log(Log.WARN, "WARN", message, error)
    fun e(message: String, error: Throwable? = null) = log(Log.ERROR, "ERROR", message, error)

    private fun log(priority: Int, level: String, message: String, error: Throwable? = null) {
        if (!dataStore.isLogging()) return
        // Exception messages and stacks can contain request URLs, credentials, or payloads.
        val diagnostic = if (error == null) message else "$message (${error.javaClass.simpleName})"
        Log.println(priority, tag, diagnostic)
        BarikoiTrace.notifyLog(level, tag, diagnostic)
    }
}
