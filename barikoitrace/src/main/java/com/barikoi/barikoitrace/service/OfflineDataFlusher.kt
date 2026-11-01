package com.barikoi.barikoitrace.service

import com.barikoi.barikoitrace.storage.OfflineLocationDao
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex

internal class OfflineDataFlusher(
    private val dao: OfflineLocationDao,
    private val publish: suspend (JsonObject) -> Unit
) {
    companion object {
        // Shared across service instances, but never persisted across process death.
        private val syncMutex = Mutex()
    }

    suspend fun flush(userId: String) {
        // Reconnect and location callbacks may request a flush at the same time.
        if (!syncMutex.tryLock()) return
        try {
            do {
                currentCoroutineContext().ensureActive()
            } while (flushBatch(userId))
        } finally {
            syncMutex.unlock()
        }
    }

    /** The publisher must return only after delivery is confirmed, or throw on failure. */
    suspend fun flushBatch(userId: String): Boolean {
        val batch = dao.getBatch()
        for (entity in batch) {
            val json = JsonParser.parseString(entity.json).asJsonObject
            if (!json.has("user_id")) json.addProperty("user_id", userId)
            publish(json)
            // Delete exactly the delivered row; concurrent inserts must survive.
            dao.deleteById(entity.id)
        }
        return batch.isNotEmpty()
    }
}
