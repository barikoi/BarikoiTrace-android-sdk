package com.barikoi.barikoitrace.service

/** Late callbacks from an old client must not overwrite the replacement client's state. */
internal object TraceConnectionState {
    private var owner: Any? = null
    @Volatile var connected: Boolean = false
        private set

    @Synchronized fun bind(token: Any) {
        owner = token
        connected = false
    }

    @Synchronized fun update(token: Any, connected: Boolean) {
        if (owner === token) this.connected = connected
    }

    @Synchronized fun clear(token: Any?) {
        if (owner === token) {
            owner = null
            connected = false
        }
    }
}
