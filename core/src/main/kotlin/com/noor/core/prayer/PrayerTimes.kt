package com.noor.core.prayer

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.tan

/**
 * Calculation methods used by Twelver Shia authorities.
 * Angles are degrees below the horizon.
 */
enum class CalculationMethod(
    val fajrAngle: Double,
    val ishaAngle: Double,
    val maghribAngle: Double,
) {
    /** Shia Ithna-Ashari, Leva Institute, Qum (Ja'fari). */
    JAFARI(fajrAngle = 16.0, ishaAngle = 14.0, maghribAngle = 4.0),

    /** Institute of Geophysics, University of Tehran. */
    TEHRAN(fajrAngle = 17.7, ishaAngle = 14.0, maghribAngle = 4.5),
}

enum class Prayer {
    FAJR, SUNRISE, DHUHR, ASR, SUNSET, MAGHRIB, ISHA, MIDNIGHT;

    /** Times that open an obligatory prayer (used for "next prayer" and notifications). */
    val isPrayer: Boolean get() = this == FAJR || this == DHUHR || this == ASR || this == MAGHRIB || this == ISHA

    companion object {
        val displayOrder = listOf(FAJR, SUNRISE, DHUHR, ASR, SUNSET, MAGHRIB, ISHA, MIDNIGHT)
        val prayers = listOf(FAJR, DHUHR, ASR, MAGHRIB, ISHA)
    }
}

data class Coordinates(val latitude: Double, val longitude: Double) {
    init {
        require(latitude in -90.0..90.0) { "latitude out of range" }
        require(longitude in -180.0..180.0) { "longitude out of range" }
    }
}

data class DailyPrayerTimes(
    val date: LocalDate,
    val zone: ZoneId,
    val times: Map<Prayer, ZonedDateTime>,
) {
    operator fun get(prayer: Prayer): ZonedDateTime? = times[prayer]
}

data class PrayerMoment(val prayer: Prayer, val time: ZonedDateTime)

/**
 * Astronomical prayer-time calculator (algorithm of PrayTimes.org, v2.3, by Hamid Zarrabi-Zadeh),
 * with the Ja'fari conventions: Asr at shadow factor 1, Maghrib by depression angle,
 * and midnight as the midpoint between sunset and Fajr.
 * Times that cannot be computed (extreme latitudes) use the angle-based high-latitude rule.
 */
class PrayerTimesCalculator(private val method: CalculationMethod = CalculationMethod.JAFARI) {

    fun calculate(date: LocalDate, coordinates: Coordinates, zone: ZoneId): DailyPrayerTimes {
        val lat = coordinates.latitude
        val lng = coordinates.longitude
        val tzOffsetHours = zone.rules.getOffset(date.atTime(LocalTime.NOON)).totalSeconds / 3600.0
        val jDate = julian(date.year, date.monthValue, date.dayOfMonth) - lng / (15.0 * 24.0)

        fun sunPosition(jd: Double): Pair<Double, Double> {
            val d = jd - 2451545.0
            val g = fixAngle(357.529 + 0.98560028 * d)
            val q = fixAngle(280.459 + 0.98564736 * d)
            val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
            val e = 23.439 - 0.00000036 * d
            val ra = darctan2(dcos(e) * dsin(l), dcos(l)) / 15.0
            val eqt = q / 15.0 - fixHour(ra)
            val decl = darcsin(dsin(e) * dsin(l))
            return decl to eqt
        }

        fun midDay(time: Double): Double = fixHour(12 - sunPosition(jDate + time).second)

        fun sunAngleTime(angle: Double, time: Double, ccw: Boolean): Double {
            val decl = sunPosition(jDate + time).first
            val noon = midDay(time)
            val t = 1.0 / 15.0 * darccos((-dsin(angle) - dsin(decl) * dsin(lat)) / (dcos(decl) * dcos(lat)))
            return noon + if (ccw) -t else t
        }

        fun asrTime(factor: Double, time: Double): Double {
            val decl = sunPosition(jDate + time).first
            val angle = -darccot(factor + dtan(abs(lat - decl)))
            return sunAngleTime(angle, time, ccw = false)
        }

        // Initial guesses (hours) converted to day portions, single iteration as in PrayTimes.
        val fajr0 = 5.0 / 24; val rise0 = 6.0 / 24; val dhuhr0 = 12.0 / 24; val asr0 = 13.0 / 24
        val set0 = 18.0 / 24
        var fajr = sunAngleTime(method.fajrAngle, fajr0, ccw = true)
        val sunrise = sunAngleTime(RISE_SET_ANGLE, rise0, ccw = true)
        var dhuhr = midDay(dhuhr0)
        var asr = asrTime(1.0, asr0)
        val sunset = sunAngleTime(RISE_SET_ANGLE, set0, ccw = false)
        var maghrib = sunAngleTime(method.maghribAngle, set0, ccw = false)
        var isha = sunAngleTime(method.ishaAngle, set0, ccw = false)

        val adj = tzOffsetHours - lng / 15.0
        fajr += adj
        val sunriseA = sunrise + adj
        dhuhr += adj
        asr += adj
        val sunsetA = sunset + adj
        maghrib += adj
        isha += adj

        // High-latitude adjustment (angle-based).
        val night = timeDiff(sunsetA, sunriseA)
        fajr = adjustHighLat(fajr, sunriseA, method.fajrAngle, night, ccw = true)
        isha = adjustHighLat(isha, sunsetA, method.ishaAngle, night, ccw = false)
        maghrib = adjustHighLat(maghrib, sunsetA, method.maghribAngle, night, ccw = false)

        val midnight = sunsetA + timeDiff(sunsetA, fajr) / 2.0

        val raw = linkedMapOf(
            Prayer.FAJR to fajr,
            Prayer.SUNRISE to sunriseA,
            Prayer.DHUHR to dhuhr,
            Prayer.ASR to asr,
            Prayer.SUNSET to sunsetA,
            Prayer.MAGHRIB to maghrib,
            Prayer.ISHA to isha,
            Prayer.MIDNIGHT to midnight,
        )
        val result = LinkedHashMap<Prayer, ZonedDateTime>()
        raw.forEach { (prayer, hours) ->
            if (!hours.isNaN()) result[prayer] = toDateTime(date, hours, zone)
        }
        return DailyPrayerTimes(date, zone, result)
    }

    /** The next obligatory prayer strictly after [now], looking into tomorrow if needed. */
    fun nextPrayer(now: ZonedDateTime, coordinates: Coordinates, zone: ZoneId): PrayerMoment? {
        val localNow = now.withZoneSameInstant(zone)
        for (offset in 0L..1L) {
            val day = calculate(localNow.toLocalDate().plusDays(offset), coordinates, zone)
            Prayer.prayers.forEach { p ->
                val t = day[p]
                if (t != null && t.isAfter(localNow)) return PrayerMoment(p, t)
            }
        }
        return null
    }

    /** The most recent obligatory prayer at or before [now]. */
    fun previousPrayer(now: ZonedDateTime, coordinates: Coordinates, zone: ZoneId): PrayerMoment? {
        val localNow = now.withZoneSameInstant(zone)
        for (offset in 0L downTo -1L) {
            val day = calculate(localNow.toLocalDate().plusDays(offset), coordinates, zone)
            Prayer.prayers.reversed().forEach { p ->
                val t = day[p]
                if (t != null && !t.isAfter(localNow)) return PrayerMoment(p, t)
            }
        }
        return null
    }

    private fun adjustHighLat(time: Double, base: Double, angle: Double, night: Double, ccw: Boolean): Double {
        val portion = angle / 60.0 * night
        val diff = if (ccw) timeDiff(time, base) else timeDiff(base, time)
        return if (time.isNaN() || diff > portion) base + if (ccw) -portion else portion else time
    }

    companion object {
        private const val RISE_SET_ANGLE = 0.833

        internal fun julian(yearIn: Int, monthIn: Int, day: Int): Double {
            var year = yearIn
            var month = monthIn
            if (month <= 2) {
                year -= 1
                month += 12
            }
            val a = floor(year / 100.0)
            val b = 2 - a + floor(a / 4.0)
            return floor(365.25 * (year + 4716)) + floor(30.6001 * (month + 1)) + day + b - 1524.5
        }

        internal fun toDateTime(date: LocalDate, hours: Double, zone: ZoneId): ZonedDateTime {
            val totalMinutes = (hours * 60.0).roundToLong()
            return LocalDateTime.of(date, LocalTime.MIDNIGHT).plusMinutes(totalMinutes).atZone(zone)
        }

        private fun fixAngle(a: Double) = a - 360.0 * floor(a / 360.0)
        private fun fixHour(a: Double) = a - 24.0 * floor(a / 24.0)
        private fun timeDiff(t1: Double, t2: Double) = fixHour(t2 - t1)
        private fun dtr(d: Double) = d * Math.PI / 180.0
        private fun rtd(r: Double) = r * 180.0 / Math.PI
        private fun dsin(d: Double) = sin(dtr(d))
        private fun dcos(d: Double) = cos(dtr(d))
        private fun dtan(d: Double) = tan(dtr(d))
        private fun darcsin(x: Double) = rtd(asin(x))
        private fun darccos(x: Double) = rtd(acos(x))
        private fun darctan2(y: Double, x: Double) = rtd(atan2(y, x))
        private fun darccot(x: Double) = rtd(atan(1.0 / x))
    }
}
