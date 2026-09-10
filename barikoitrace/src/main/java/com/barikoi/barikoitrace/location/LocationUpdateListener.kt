package com.barikoi.barikoitrace.location

import android.location.Location
import com.barikoi.barikoitrace.model.TraceError

interface LocationUpdateListener {
    fun onLocationReceived(location: Location)
    fun onFailure(error: TraceError)
    fun onProviderAvailabilityChanged(available: Boolean)
}
