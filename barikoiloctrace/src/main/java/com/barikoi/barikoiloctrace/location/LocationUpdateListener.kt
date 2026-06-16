package com.barikoi.barikoiloctrace.location

import android.location.Location
import com.barikoi.barikoiloctrace.model.TraceError

interface LocationUpdateListener {
    fun onLocationReceived(location: Location)
    fun onFailure(error: TraceError)
    fun onProviderAvailabilityChanged(available: Boolean)
}
