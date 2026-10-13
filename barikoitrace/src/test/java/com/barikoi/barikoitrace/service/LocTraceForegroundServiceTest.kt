package com.barikoi.barikoitrace.service

import android.app.Service
import android.content.Intent
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocTraceForegroundServiceTest {

    @Test
    fun onStartCommand_withModeChangedAction_returnsStickyWithoutCrashing() {
        val controller = Robolectric.buildService(LocTraceForegroundService::class.java).create()
        val service = controller.get()
        val intent = Intent(service, LocTraceForegroundService::class.java).apply {
            action = LocTraceForegroundService.ACTION_TRACE_MODE_CHANGED
        }

        val result = service.onStartCommand(intent, 0, 0)

        assertThat(result).isEqualTo(Service.START_STICKY)
        controller.destroy()
    }

    @Test
    fun onStartCommand_withUpdateUserNameAction_returnsStickyWithoutCrashing() {
        val controller = Robolectric.buildService(LocTraceForegroundService::class.java).create()
        val service = controller.get()
        val intent = Intent(service, LocTraceForegroundService::class.java).apply {
            action = LocTraceForegroundService.ACTION_UPDATE_USER_NAME
        }

        val result = service.onStartCommand(intent, 0, 0)

        assertThat(result).isEqualTo(Service.START_STICKY)
        controller.destroy()
    }
}
