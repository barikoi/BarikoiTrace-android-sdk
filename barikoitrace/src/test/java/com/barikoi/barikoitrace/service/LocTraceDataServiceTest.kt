package com.barikoi.barikoitrace.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import com.barikoi.barikoitrace.storage.OfflineLocationDb
import com.barikoi.barikoitrace.storage.TraceDataStore
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocTraceDataServiceTest {
    @Test
    fun disabledOfflineTracking_skipsLocationCollectionAndForegroundWork() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        TraceDataStore(context).setOfflineTracking(false)
        val dao = OfflineLocationDb.getInstance(context).locationDao()
        dao.deleteAll()
        // No WorkManager foreground infrastructure is needed when offline collection is disabled.
        val worker = LocTraceDataService(context, mockk(relaxed = true))
        assertThat(worker.doWork()).isEqualTo(ListenableWorker.Result.success())
        assertThat(dao.getCount()).isEqualTo(0)
    }
}
