package com.barikoi.barikoitrace.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TraceErrorTest {

    @Test
    fun noUserError_hasCorrectCodeAndMessage() {
        val error = TraceError.noUserError()
        assertThat(error.code).isEqualTo("NO_USER")
        assertThat(error.message).contains("user")
    }

    @Test
    fun noKeyError_hasCorrectCodeAndMessage() {
        val error = TraceError.noKeyError()
        assertThat(error.code).isEqualTo("NO_KEY")
        assertThat(error.message).contains("API key")
    }

    @Test
    fun noDataError_hasCorrectCodeAndMessage() {
        val error = TraceError.noDataError()
        assertThat(error.code).isEqualTo("NO_DATA")
        assertThat(error.message).contains("missing")
    }

    @Test
    fun networkError_hasCorrectCodeAndMessage() {
        val error = TraceError.networkError()
        assertThat(error.code).isEqualTo("NETWORK")
        assertThat(error.message).contains("network")
    }

    @Test
    fun locationPermissionError_hasCorrectCodeAndMessage() {
        val error = TraceError.locationPermissionError()
        assertThat(error.code).isEqualTo("PERMISSION")
        assertThat(error.message).contains("permission")
    }

    @Test
    fun locationNotFoundError_hasCorrectCodeAndMessage() {
        val error = TraceError.locationNotFoundError()
        assertThat(error.code).isEqualTo("LOCATION")
        assertThat(error.message).contains("location")
    }

    @Test
    fun serverError_hasCorrectCodeAndMessage() {
        val error = TraceError.serverError()
        assertThat(error.code).isEqualTo("SERVER")
        assertThat(error.message).contains("Server")
    }

    @Test
    fun tripStateError_hasCorrectCodeAndMessage() {
        val error = TraceError.tripStateError()
        assertThat(error.code).isEqualTo("TRIP")
        assertThat(error.message).contains("trip")
    }

    @Test
    fun mockAppError_hasCorrectCodeAndMessage() {
        val error = TraceError.mockAppError()
        assertThat(error.code).isEqualTo("MOCK")
        assertThat(error.message).contains("Mock")
    }

    @Test
    fun jsonError_includesDetailInMessage() {
        val error = TraceError.jsonError("unexpected token")
        assertThat(error.code).isEqualTo("JSON")
        assertThat(error.message).contains("unexpected token")
    }
}
