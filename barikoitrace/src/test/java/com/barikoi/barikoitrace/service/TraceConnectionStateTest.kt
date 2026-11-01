package com.barikoi.barikoitrace.service

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TraceConnectionStateTest {
    @Test fun staleCallbacksAndDestructionCannotOverwriteReplacementConnection() {
        val old = Any()
        val current = Any()
        TraceConnectionState.bind(old)
        TraceConnectionState.update(old, true)
        assertThat(TraceConnectionState.connected).isTrue()
        TraceConnectionState.bind(current)
        TraceConnectionState.update(old, true)
        assertThat(TraceConnectionState.connected).isFalse()
        TraceConnectionState.update(current, true)
        TraceConnectionState.clear(old)
        assertThat(TraceConnectionState.connected).isTrue()
        TraceConnectionState.clear(current)
        assertThat(TraceConnectionState.connected).isFalse()
        TraceConnectionState.update(current, true)
        assertThat(TraceConnectionState.connected).isFalse()
    }
}
