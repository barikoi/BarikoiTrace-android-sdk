package com.barikoi.barikoitrace.service

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.barikoi.barikoitrace.storage.OfflineLocationDb
import com.barikoi.barikoitrace.storage.OfflineLocationEntity
import com.barikoi.barikoitrace.storage.TraceDataStore
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class OfflineDataFlusherTest {
    private lateinit var db: OfflineLocationDb

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(), OfflineLocationDb::class.java
        ).build()
    }

    @After
    fun teardown() = db.close()

    @Test
    fun pendingDelivery_keepsRowUntilAcknowledgedAndPreservesConcurrentInsert(): Unit = runBlocking {
        val dao = db.locationDao()
        dao.insert(OfflineLocationEntity(json = """{"user_id":"original"}"""))
        val started = CompletableDeferred<Unit>()
        val delivered = CompletableDeferred<Unit>()
        val flush = async {
            OfflineDataFlusher(dao) { json ->
                assertThat(json.get("user_id").asString).isEqualTo("original")
                started.complete(Unit)
                delivered.await()
            }.flushBatch("current")
        }
        started.await()
        assertThat(dao.getCount()).isEqualTo(1)
        dao.insert(OfflineLocationEntity(json = "{}"))
        val newRowId = dao.getBatch().last().id
        delivered.complete(Unit)
        assertThat(flush.await()).isTrue()
        assertThat(dao.getBatch().map { it.id }).containsExactly(newRowId)
    }

    @Test
    fun failedDelivery_retainsFailedAndUnattemptedRowsAndAllowsRetry() = runBlocking {
        val dao = db.locationDao()
        repeat(3) { dao.insert(OfflineLocationEntity(json = "{}")) }
        val originalIds = dao.getBatch().map { it.id }
        var attempts = 0
        try {
            OfflineDataFlusher(dao) {
                attempts++
                if (attempts == 2) throw IOException("Connection lost")
            }.flushBatch("current")
            throw AssertionError("Expected publish failure")
        } catch (_: IOException) {}
        assertThat(attempts).isEqualTo(2)
        assertThat(dao.getBatch().map { it.id }).containsExactlyElementsIn(originalIds.drop(1))
        OfflineDataFlusher(dao) { json ->
            assertThat(json.get("user_id").asString).isEqualTo("current")
        }.flushBatch("current")
        assertThat(dao.getCount()).isEqualTo(0)
    }

    @Test
    fun malformedRow_isRetainedWithoutPublishingOrDeletingLaterRows() = runBlocking {
        val dao = db.locationDao()
        dao.insert(OfflineLocationEntity(json = "[]"))
        dao.insert(OfflineLocationEntity(json = "{}"))
        var published = false
        try {
            OfflineDataFlusher(dao) { published = true }.flushBatch("current")
            throw AssertionError("Expected malformed row failure")
        } catch (_: IllegalStateException) {}
        assertThat(published).isFalse()
        assertThat(dao.getCount()).isEqualTo(2)
    }

    @Test
    fun simultaneousFlushesAcrossInstances_publishEachRowOnce() = runBlocking {
        val dao = db.locationDao()
        repeat(101) { dao.insert(OfflineLocationEntity(json = "{}")) }
        val started = CompletableDeferred<Unit>()
        val delivered = CompletableDeferred<Unit>()
        var publishes = 0
        val first = async {
            OfflineDataFlusher(dao) {
                publishes++
                started.complete(Unit)
                delivered.await()
            }.flush("current")
        }
        started.await()
        var competingPublishes = 0
        OfflineDataFlusher(dao) { competingPublishes++ }.flush("current")
        assertThat(competingPublishes).isEqualTo(0)
        assertThat(dao.getCount()).isEqualTo(101)
        delivered.complete(Unit)
        first.await()
        assertThat(publishes).isEqualTo(101)
        assertThat(dao.getCount()).isEqualTo(0)
    }

    @Test
    fun failedFlush_releasesLockForRetry() = runBlocking {
        val dao = db.locationDao()
        dao.insert(OfflineLocationEntity(json = "{}"))
        try {
            OfflineDataFlusher(dao) { throw IOException("Disconnected") }.flush("current")
            throw AssertionError("Expected publish failure")
        } catch (_: IOException) {}
        assertThat(dao.getCount()).isEqualTo(1)
        OfflineDataFlusher(dao) {}.flush("current")
        assertThat(dao.getCount()).isEqualTo(0)
    }

    @Test
    fun cancelledFlush_releasesLockAndRetainsUnconfirmedRow() = runBlocking {
        val dao = db.locationDao()
        dao.insert(OfflineLocationEntity(json = "{}"))
        val started = CompletableDeferred<Unit>()
        val delivered = CompletableDeferred<Unit>()
        val first = async {
            OfflineDataFlusher(dao) {
                started.complete(Unit)
                delivered.await()
            }.flush("current")
        }
        started.await()
        first.cancelAndJoin()
        assertThat(dao.getCount()).isEqualTo(1)
        OfflineDataFlusher(dao) {}.flush("current")
        assertThat(dao.getCount()).isEqualTo(0)
    }

    @Test
    fun stalePersistedSyncFlag_doesNotBlockFlush() = runBlocking {
        val dataStore = TraceDataStore(ApplicationProvider.getApplicationContext<Context>())
        dataStore.setDataSyncing(true)
        try {
            val dao = db.locationDao()
            dao.insert(OfflineLocationEntity(json = "{}"))
            var publishes = 0
            OfflineDataFlusher(dao) { publishes++ }.flush("current")
            assertThat(publishes).isEqualTo(1)
            assertThat(dao.getCount()).isEqualTo(0)
        } finally {
            dataStore.setDataSyncing(false)
        }
    }
}
