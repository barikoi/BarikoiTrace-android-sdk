package com.barikoi.barikoitrace

/**
 * Decides which trace-mode config the SDK uses when tracking starts.
 */
enum class TraceModeSource {
    /** The [TraceMode] passed to startTracking is stored and used. */
    LOCAL,

    /** Company settings fetched from the remote API win; the passed mode is only a fallback. */
    REMOTE
}
