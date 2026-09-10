package com.barikoi.barikoitrace.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class DateTimeUtilsTest {

    @Test
    fun getDateTimeLocal_returnsUtcFormattedString() {
        val timestamp = 1700000000000L // Known epoch millis
        val result = DateTimeUtils.getDateTimeLocal(timestamp)

        val expected = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(java.util.Date(timestamp))

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun getDateTimeLocal_formatHasCorrectPattern() {
        val result = DateTimeUtils.getDateTimeLocal(1700000000000L)
        // Should match yyyy-MM-dd HH:mm:ss pattern
        assertThat(result).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")
    }

    @Test
    fun getCurrentTimeLocal_returnsValidFormat() {
        val result = DateTimeUtils.getCurrentTimeLocal()
        assertThat(result).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")
    }

    @Test
    fun getDateTimeLocal_isUtcTimezone() {
        // 0 epochs should be 1970-01-01 00:00:00 in UTC
        val result = DateTimeUtils.getDateTimeLocal(0L)
        assertThat(result).isEqualTo("1970-01-01 00:00:00")
    }
}
