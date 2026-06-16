package com.barikoi.barikoiloctrace.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateTimeUtils {

    private val utcFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun getDateTimeLocal(time: Long): String {
        return utcFormat.format(Date(time))
    }

    fun getCurrentTimeLocal(): String {
        return utcFormat.format(Date())
    }
}
