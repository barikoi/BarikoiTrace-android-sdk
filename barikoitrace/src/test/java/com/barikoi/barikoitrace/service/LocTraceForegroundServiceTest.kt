package com.barikoi.barikoitrace.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Build
import com.barikoi.barikoitrace.BarikoiTrace
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowService

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
    fun onDestroy_withoutStartCommand_doesNotThrow() {
        val controller = Robolectric.buildService(LocTraceForegroundService::class.java).create()

        controller.destroy()
    }

    @Test
    fun onDestroy_afterUpdateUserNameEarlyReturn_doesNotThrow() {
        val controller = Robolectric.buildService(LocTraceForegroundService::class.java).create()
        val service = controller.get()
        val intent = Intent(service, LocTraceForegroundService::class.java).apply {
            action = LocTraceForegroundService.ACTION_UPDATE_USER_NAME
        }

        service.onStartCommand(intent, 0, 0)

        controller.destroy()
    }

    @Test
    @Config(sdk = [34], shadows = [DenyingForegroundShadowService::class])
    fun onCreate_whenForegroundStartDenied_stopsSelfWithoutCrashing() {
        val levels = mutableListOf<String>()
        BarikoiTrace.setLogListener(object : BarikoiTrace.TraceLogListener {
            override fun onLog(level: String, tag: String, message: String) {
                levels.add(level)
            }
        })
        try {
            val controller = Robolectric.buildService(LocTraceForegroundService::class.java).create()

            assertThat(Shadows.shadowOf(controller.get()).isStoppedBySelf()).isTrue()
            assertThat(levels).contains("ERROR")
        } finally {
            BarikoiTrace.setLogListener(null)
        }
    }

    @Test
    @Config(sdk = [34], shadows = [DenyingForegroundShadowService::class])
    fun onStartCommand_whenForegroundStartDenied_returnsNotStickyAndStopsSelf() {
        val controller = Robolectric.buildService(LocTraceForegroundService::class.java).create()
        val service = controller.get()

        val result = service.onStartCommand(Intent(service, LocTraceForegroundService::class.java), 0, 0)

        assertThat(result).isEqualTo(Service.START_NOT_STICKY)
        assertThat(Shadows.shadowOf(service).isStoppedBySelf()).isTrue()
    }
}

/**
 * Simulates the Android 12+ foreground-service background-start restriction:
 * both startForeground overloads throw, like ForegroundServiceStartNotAllowedException
 * (an IllegalStateException subclass) does on a denied promotion.
 */
@Implements(android.app.Service::class)
class DenyingForegroundShadowService : ShadowService() {

    @Implementation
    override fun startForeground(id: Int, notification: Notification?) {
        throw IllegalStateException("startForeground() not allowed due to mAllowStartForeground false")
    }

    @Implementation(minSdk = Build.VERSION_CODES.Q)
    override fun startForeground(id: Int, notification: Notification?, foregroundServiceType: Int) {
        throw IllegalStateException("startForeground() not allowed due to mAllowStartForegroundColor false")
    }
}
