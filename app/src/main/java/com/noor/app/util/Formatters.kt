package com.noor.app.util

import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formatters {
    private const val ARABIC_DIGITS = "٠١٢٣٤٥٦٧٨٩"

    fun digits(text: String, arabic: Boolean): String {
        if (!arabic) return text
        val sb = StringBuilder(text.length)
        text.forEach { c -> sb.append(if (c in '0'..'9') ARABIC_DIGITS[c - '0'] else c) }
        return sb.toString()
    }

    fun number(value: Number, arabic: Boolean): String = digits(value.toString(), arabic)

    private fun locale(arabic: Boolean): Locale = if (arabic) Locale.forLanguageTag("ar") else Locale.ENGLISH

    fun time(time: ZonedDateTime, arabic: Boolean): String =
        digits(time.format(DateTimeFormatter.ofPattern("h:mm a", locale(arabic))), arabic)

    fun gregorianLong(date: LocalDate, arabic: Boolean): String {
        val pattern = if (arabic) "EEEE، d MMMM yyyy" else "EEEE, d MMMM yyyy"
        return digits(date.format(DateTimeFormatter.ofPattern(pattern, locale(arabic))), arabic)
    }

    fun gregorianShort(date: LocalDate, arabic: Boolean): String =
        digits(date.format(DateTimeFormatter.ofPattern("d MMM", locale(arabic))), arabic)

    fun weekdayShort(date: LocalDate, arabic: Boolean): String =
        date.format(DateTimeFormatter.ofPattern(if (arabic) "EEEE" else "EEE", locale(arabic)))

    /** hh:mm:ss countdown, never negative. */
    fun countdown(duration: Duration, arabic: Boolean): String {
        val total = duration.seconds.coerceAtLeast(0)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return digits("%02d:%02d:%02d".format(Locale.ENGLISH, h, m, s), arabic)
    }
}
