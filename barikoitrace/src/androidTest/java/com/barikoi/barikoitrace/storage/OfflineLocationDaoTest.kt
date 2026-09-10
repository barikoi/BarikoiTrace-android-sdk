package com.barikoi.barikoitrace.storage

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineLocationDaoTest {

    private lateinit var db: OfflineLocationDb
    private lateinit var dao: OfflineLocationDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, OfflineLocationDb::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.locationDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insert_storesRecordAndCountReflectsIt() = runBlocking {
        dao.insert(OfflineLocationEntity(json = """{"lat":1.0}"""))
        assertThat(dao.getCount()).isEqualTo(1)
    }

    @Test
    fun getBatch_returnsEmptyWhenTableIsEmpty() = runBlocking {
        val batch = dao.getBatch()
        assertThat(batch).isEmpty()
    }

    @Test
    fun getBatch_returnsUpTo100RecordsOrderedByIdAsc() = runBlocking {
        for (i in 1..150) {
            dao.insert(OfflineLocationEntity(json = """{"id":$i}"""))
        }
        val batch = dao.getBatch()
        assertThat(batch).hasSize(100)
        // Verify ASC ordering: first record should have id=1 (first inserted)
        assertThat(batch[0].id).isLessThan(batch[1].id)
    }

    @Test
    fun deleteBatch_removesExactly100Records() = runBlocking {
        for (i in 1..250) {
            dao.insert(OfflineLocationEntity(json = """{"id":$i}"""))
        }
        assertThat(dao.getCount()).isEqualTo(250)

        val deleted = dao.deleteBatch()
        assertThat(deleted).isEqualTo(100)
        assertThat(dao.getCount()).isEqualTo(150)
    }

    @Test
    fun deleteBatch_afterGetBatch_removesCorrectBatch() = runBlocking {
        for (i in 1..250) {
            dao.insert(OfflineLocationEntity(json = """{"id":$i}"""))
        }

        val firstBatch = dao.getBatch()
        assertThat(firstBatch).hasSize(100)

        dao.deleteBatch()
        assertThat(dao.getCount()).isEqualTo(150)

        // Second batch should have different (later) records
        val secondBatch = dao.getBatch()
        assertThat(secondBatch).hasSize(100)
        assertThat(secondBatch[0].id).isGreaterThan(firstBatch.last().id)
    }

    @Test
    fun deleteBatch_whenFewerThan100_deletesAll() = runBlocking {
        for (i in 1..50) {
            dao.insert(OfflineLocationEntity(json = """{"id":$i}"""))
        }
        dao.deleteBatch()
        assertThat(dao.getCount()).isEqualTo(0)
    }
}
