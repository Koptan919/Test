package com.noor.core

import com.noor.core.calendar.AdjustedHijriConverter
import com.noor.core.calendar.HijriDate
import com.noor.core.calendar.Occasions
import com.noor.core.calendar.TabularHijriConverter
import com.noor.core.calendar.UmmAlQuraConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HijriTest {
    @Test
    fun ummAlQuraKnownDates() {
        assertEquals(HijriDate(1445, 9, 1), UmmAlQuraConverter.toHijri(LocalDate.of(2024, 3, 11)))
        assertEquals(HijriDate(1447, 1, 1), UmmAlQuraConverter.toHijri(LocalDate.of(2025, 6, 26)))
        assertEquals(LocalDate.of(2024, 3, 11), UmmAlQuraConverter.toGregorian(HijriDate(1445, 9, 1)))
    }

    @Test
    fun tabularRoundTripAndCloseToUmmAlQura() {
        var d = LocalDate.of(2000, 1, 1)
        while (d.isBefore(LocalDate.of(2040, 1, 1))) {
            val h = TabularHijriConverter.toHijri(d)
            assertTrue(h.day <= TabularHijriConverter.monthLength(h.year, h.month))
            assertEquals(d, TabularHijriConverter.toGregorian(h))
            val u = UmmAlQuraConverter.toGregorian(h.copy(day = minOf(h.day, 29)))
            val tg = TabularHijriConverter.toGregorian(h.copy(day = minOf(h.day, 29)))
            assertTrue("$h", kotlin.math.abs(u.toEpochDay() - tg.toEpochDay()) <= 3)
            d = d.plusDays(13)
        }
    }

    @Test
    fun adjustmentShiftsDate() {
        val plusOne = AdjustedHijriConverter(UmmAlQuraConverter, 1)
        assertEquals(HijriDate(1445, 9, 2), plusOne.toHijri(LocalDate.of(2024, 3, 11)))
        assertEquals(LocalDate.of(2024, 3, 10), plusOne.toGregorian(HijriDate(1445, 9, 1)))
    }

    @Test
    fun occasionsFoundOnTheirDays() {
        val ashura = UmmAlQuraConverter.toGregorian(HijriDate(1447, 1, 10))
        val found = Occasions.on(UmmAlQuraConverter.toHijri(ashura), UmmAlQuraConverter)
        assertTrue(found.any { it.titleEn.startsWith("Ashura") })
        Occasions.all.forEach { assertTrue(it.day in 1..30 && it.month in 1..12) }
    }
}
