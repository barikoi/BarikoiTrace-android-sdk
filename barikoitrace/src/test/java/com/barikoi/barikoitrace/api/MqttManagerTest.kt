package com.barikoi.barikoitrace.api

import android.content.Context
import android.location.Location
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MqttManagerTest {

    private fun createManager(userName: String? = null): MqttManager {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return MqttManager(
            context, "tcp://test:1883", "u1", "c1", "g1", "uuid-1",
            callback = null, userName = userName
        )
    }

    private fun testLocation(): Location =
        Location("test").apply {
            latitude = 23.7925
            longitude = 90.4078
            time = 1700000000000L
        }

    @Test
    fun updateUserName_updatesPayloadUserName() {
        val manager = createManager(userName = "Old")
        manager.updateUserName("New")
        val json = manager.buildLocationJson(testLocation())
        assertThat(json.get("user_name").asString).isEqualTo("New")
    }

    @Test
    fun buildLocationJson_initialUserNameIncluded() {
        val manager = createManager(userName = "Initial")
        val json = manager.buildLocationJson(testLocation())
        assertThat(json.get("user_name").asString).isEqualTo("Initial")
    }

    @Test
    fun buildLocationJson_blankUserName_omitted() {
        val manager = createManager(userName = "Old")
        manager.updateUserName("   ")
        val json = manager.buildLocationJson(testLocation())
        assertThat(json.has("user_name")).isFalse()
    }

    @Test
    fun buildLocationJson_containsCoreFields() {
        val manager = createManager(userName = "Tester")
        val json = manager.buildLocationJson(testLocation(), tripId = "trip-1")
        assertThat(json.get("user_id").asString).isEqualTo("u1")
        assertThat(json.get("company_id").asString).isEqualTo("c1")
        assertThat(json.get("latitude").asString).isEqualTo("23.7925")
        assertThat(json.get("trip_id").asString).isEqualTo("trip-1")
        assertThat(json.get("trip_status").asString).isEqualTo("active")
    }
}
