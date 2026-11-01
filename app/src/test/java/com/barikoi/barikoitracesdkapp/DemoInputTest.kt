package com.barikoi.barikoitracesdkapp

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class DemoInputTest {
    @Test fun restUrl_normalizesWhitespaceAndTrailingSlash() {
        assertThat(DemoInput.restUrl(" https://example.com/api/v1 ")).isEqualTo("https://example.com/api/v1/")
    }
    @Test fun restUrl_rejectsMissingHostCredentialsQueryAndBadPort() {
        listOf("", "example.com", "tcp://example.com", "https://user:secret@example.com", "https://example.com/?api_key=secret", "https://example.com:70000").forEach {
            assertThrows(IllegalArgumentException::class.java) { DemoInput.restUrl(it) }
        }
    }
    @Test fun mqttUrl_acceptsTcpAndWebSocketPaths() {
        assertThat(DemoInput.mqttUrl(" tcp://example.com:1883 ")).isEqualTo("tcp://example.com:1883")
        assertThat(DemoInput.mqttUrl("wss://example.com/mqtt")).isEqualTo("wss://example.com/mqtt")
    }
    @Test fun mqttUrl_rejectsInvalidProtocolsCredentialsAndTcpPath() {
        listOf("mqtt://example.com", "tcp://", "tcp://user:secret@example.com:1883", "tcp://example.com/path", "tcp://example.com:0").forEach {
            assertThrows(IllegalArgumentException::class.java) { DemoInput.mqttUrl(it) }
        }
    }
    @Test fun customMode_preservesDistanceTrackingAndOfflineChoice() {
        val mode = DemoInput.mode("CUSTOM", "0", "100", "150", false)
        assertThat(mode.updateInterval).isEqualTo(0)
        assertThat(mode.distanceFilter).isEqualTo(100)
        assertThat(mode.offline).isFalse()
    }
    @Test fun customMode_rejectsAmbiguousOrInvalidInputs() {
        listOf(listOf("10", "100", "100"), listOf("1", "0", "100"), listOf("0", "1", "100"), listOf("10", "0", "1"), listOf("", "0", "100")).forEach {
            assertThrows(IllegalArgumentException::class.java) { DemoInput.mode("CUSTOM", it[0], it[1], it[2], true) }
        }
    }
    @Test fun preset_ignoresDisabledCustomFieldsAndHonorsOfflineChoice() {
        val mode = DemoInput.mode("PASSIVE", "bad", "bad", "bad", false)
        assertThat(mode.updateInterval).isEqualTo(0)
        assertThat(mode.distanceFilter).isEqualTo(100)
        assertThat(mode.offline).isFalse()
    }
}
