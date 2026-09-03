package com.barikoi.barikoitrace.model

data class TraceError(
    val code: String,
    val message: String
) {
    companion object {
        fun noUserError() = TraceError("NO_USER", "No user found. Create a user first.")
        fun noKeyError() = TraceError("NO_KEY", "API key not set. Call initialize() first.")
        fun noDataError() = TraceError("NO_DATA", "Required data is missing.")
        fun networkError() = TraceError("NETWORK", "No network connection available.")
        fun locationPermissionError() = TraceError("PERMISSION", "Location permission not granted.")
        fun locationNotFoundError() = TraceError("LOCATION", "Could not determine location.")
        fun serverError() = TraceError("SERVER", "Server error occurred.")
        fun tripStateError() = TraceError("TRIP", "Not currently on a trip.")
        fun mockAppError() = TraceError("MOCK", "Mock location detected. Please disable mock location.")
        fun jsonError(detail: String) = TraceError("JSON", "JSON parsing error: $detail")

        /**
         * The authenticated account has no company association, so no MQTT
         * topic can be resolved for it. Was a bare `Exception("Company not
         * found")`; the iOS SDK has always had this code.
         */
        fun noCompanyError() = TraceError("NO_COMPANY", "User has no company association.")
    }
}
