package com.noor.core.calendar

import java.time.LocalDate
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min

data class HijriDate(val year: Int, val month: Int, val day: Int) : Comparable<HijriDate> {
    init {
        require(month in 1..12) { "month must be 1..12" }
        require(day in 1..30) { "day must be 1..30" }
    }

    override fun compareTo(other: HijriDate): Int =
        compareValuesBy(this, other, HijriDate::year, HijriDate::month, HijriDate::day)
}

interface HijriConverter {
    fun toHijri(date: LocalDate): HijriDate
    fun toGregorian(date: HijriDate): LocalDate
    fun monthLength(year: Int, month: Int): Int
}

/** Arithmetic (tabular) Islamic calendar, civil epoch. Deterministic fallback. */
object TabularHijriConverter : HijriConverter {
    private const val EPOCH_JDN = 1948440L
    private const val UNIX_EPOCH_JDN = 2440588L

    private fun jdn(y: Int, m: Int, d: Int): Long =
        d + ceil(29.5 * (m - 1)).toLong() + (y - 1) * 354L + floor((3 + 11.0 * y) / 30.0).toLong() + EPOCH_JDN - 1

    override fun toHijri(date: LocalDate): HijriDate {
        val jdn = date.toEpochDay() + UNIX_EPOCH_JDN
        val year = floor((30.0 * (jdn - EPOCH_JDN) + 10646) / 10631.0).toInt()
        val month = min(12.0, ceil((jdn - (29 + jdn(year, 1, 1))) / 29.5) + 1).toInt().coerceAtLeast(1)
        val day = (jdn - jdn(year, month, 1) + 1).toInt()
        return HijriDate(year, month, day)
    }

    override fun toGregorian(date: HijriDate): LocalDate =
        LocalDate.ofEpochDay(jdn(date.year, date.month, date.day) - UNIX_EPOCH_JDN)

    override fun monthLength(year: Int, month: Int): Int = when {
        month % 2 == 1 -> 30
        month != 12 -> 29
        (14 + 11 * year) % 30 < 11 -> 30
        else -> 29
    }
}

/**
 * Umm al-Qura calendar from java.time ([HijrahChronology]); falls back to the tabular
 * calendar outside its supported range or if the platform lacks the data.
 */
object UmmAlQuraConverter : HijriConverter {
    private val available: Boolean = runCatching { HijrahDate.from(LocalDate.of(2024, 1, 1)) }.isSuccess

    override fun toHijri(date: LocalDate): HijriDate {
        if (!available) return TabularHijriConverter.toHijri(date)
        return runCatching {
            val h = HijrahChronology.INSTANCE.date(date)
            HijriDate(h.get(ChronoField.YEAR), h.get(ChronoField.MONTH_OF_YEAR), h.get(ChronoField.DAY_OF_MONTH))
        }.getOrElse { TabularHijriConverter.toHijri(date) }
    }

    override fun toGregorian(date: HijriDate): LocalDate {
        if (!available) return TabularHijriConverter.toGregorian(date)
        return runCatching { LocalDate.from(HijrahDate.of(date.year, date.month, date.day)) }
            .getOrElse { TabularHijriConverter.toGregorian(date) }
    }

    override fun monthLength(year: Int, month: Int): Int {
        if (!available) return TabularHijriConverter.monthLength(year, month)
        return runCatching { HijrahDate.of(year, month, 1).lengthOfMonth() }
            .getOrElse { TabularHijriConverter.monthLength(year, month) }
    }
}

/**
 * Applies a user-chosen day offset (moon-sighting differences): a positive
 * [adjustDays] makes the Hijri date later than the base calendar.
 */
class AdjustedHijriConverter(
    private val base: HijriConverter = UmmAlQuraConverter,
    private val adjustDays: Int = 0,
) : HijriConverter {
    override fun toHijri(date: LocalDate): HijriDate = base.toHijri(date.plusDays(adjustDays.toLong()))
    override fun toGregorian(date: HijriDate): LocalDate = base.toGregorian(date).minusDays(adjustDays.toLong())
    override fun monthLength(year: Int, month: Int): Int = base.monthLength(year, month)
}

object HijriMonths {
    val arabic = listOf(
        "مُحَرَّم", "صَفَر", "رَبيع الأوّل", "رَبيع الثاني", "جُمادى الأولى", "جُمادى الآخرة",
        "رَجَب", "شَعبان", "رَمَضان", "شَوّال", "ذو القَعدة", "ذو الحِجّة",
    )
    val english = listOf(
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani", "Jumada al-Ula", "Jumada al-Akhirah",
        "Rajab", "Sha'ban", "Ramadan", "Shawwal", "Dhu al-Qa'dah", "Dhu al-Hijjah",
    )

    fun name(month: Int, arabicLanguage: Boolean): String =
        (if (arabicLanguage) arabic else english)[month - 1]
}
