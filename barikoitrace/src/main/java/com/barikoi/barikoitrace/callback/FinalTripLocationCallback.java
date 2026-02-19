package com.barikoi.barikoitrace.callback;

/**
 * Callback interface for sending final trip location when a trip ends.
 * Replaces the deprecated LocalBroadcastManager pattern.
 */
public interface FinalTripLocationCallback {

    /**
     * Called when the final trip location needs to be sent via MQTT.
     *
     * @param tripId     The ID of the trip being ended
     * @param tripStatus The status of the trip (e.g., "completed")
     */
    void onSendFinalLocation(String tripId, String tripStatus);
}
