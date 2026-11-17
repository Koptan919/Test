package com.noor.core

import com.noor.core.location.Cities
import com.noor.core.prayer.CalculationMethod
import com.noor.core.prayer.Coordinates
import com.noor.core.prayer.Prayer
import com.noor.core.prayer.PrayerTimesCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class PrayerTimesTest {
    private val calc = PrayerTimesCalculator(CalculationMethod.JAFARI)

    private fun assertNear(expected: LocalTime, actual: ZonedDateTime?, toleranceMin: Long = 3) {
        assertNotNull(actual)
        val diff = Duration.between(expected, actual!!.toLocalTime()).toMinutes()
        assertTrue("expected ~$expected but was ${actual.toLocalTime()}", kotlin.math.abs(diff) <= toleranceMin)
    }

    @Test
    fun londonSummerSolsticeSunriseSunsetAndNoon() {
        val zone = ZoneId.of("Europe/London")
        val t = calc.calculate(LocalDate.of(2024, 6, 21), Coordinates(51.5074, -0.1278), zone)
        assertNear(LocalTime.of(4, 43), t[Prayer.SUNRISE])
        assertNear(LocalTime.of(21, 21), t[Prayer.SUNSET])
        assertNear(LocalTime.of(13, 2), t[Prayer.DHUHR])
    }

    @Test
    fun allCitiesProduceOrderedTimesAllYear() {
        Cities.all.forEach { city ->
            val zone = ZoneId.of(city.zoneId)
            var date = LocalDate.of(2026, 1, 1)
            while (date.year == 2026) {
                val t = calc.calculate(date, city.coordinates, zone)
                val order = listOf(Prayer.FAJR, Prayer.SUNRISE, Prayer.DHUHR, Prayer.ASR, Prayer.SUNSET, Prayer.MAGHRIB, Prayer.ISHA)
                order.forEach { assertNotNull("${city.id} $date $it", t[it]) }
                order.zipWithNext().forEach { (a, b) ->
                    assertTrue("${city.id} $date $a=${t[a]} !< $b=${t[b]}", t[a]!!.isBefore(t[b]!!))
                }
                assertTrue(t[Prayer.MIDNIGHT]!!.isAfter(t[Prayer.SUNSET]!!))
                date = date.plusDays(7)
            }
        }
    }

    @Test
    fun jafariMaghribIsAfterSunsetByDepressionAngle() {
        val najaf = Cities.byId("najaf")!!
        val t = calc.calculate(LocalDate.of(2026, 3, 20), najaf.coordinates, ZoneId.of(najaf.zoneId))
        val gap = Duration.between(t[Prayer.SUNSET], t[Prayer.MAGHRIB]).toMinutes()
        assertTrue("gap=$gap", gap in 12..22)
        assertNear(LocalTime.of(12, 10), t[Prayer.DHUHR], 4)
        println("Najaf 2026-03-20: " + Prayer.displayOrder.joinToString { "$it ${t[it]?.toLocalTime()}" })
    }

    @Test
    fun nextPrayerRollsOverToTomorrowFajr() {
        val city = Cities.byId("qom")!!
        val zone = ZoneId.of(city.zoneId)
        val lateNight = ZonedDateTime.of(2026, 5, 1, 23, 50, 0, 0, zone)
        val next = calc.nextPrayer(lateNight, city.coordinates, zone)!!
        assertEquals(Prayer.FAJR, next.prayer)
        assertEquals(LocalDate.of(2026, 5, 2), next.time.toLocalDate())
        val prev = calc.previousPrayer(lateNight, city.coordinates, zone)!!
        assertEquals(Prayer.ISHA, prev.prayer)
    }
}
