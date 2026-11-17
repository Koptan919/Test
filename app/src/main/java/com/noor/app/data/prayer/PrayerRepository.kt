package com.noor.app.data.prayer

import com.noor.app.data.prefs.AppSettings
import com.noor.core.location.Cities
import com.noor.core.prayer.Coordinates
import com.noor.core.prayer.DailyPrayerTimes
import com.noor.core.prayer.PrayerMoment
import com.noor.core.prayer.PrayerTimesCalculator
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

data class PrayerLocation(
    val nameAr: String,
    val nameEn: String,
    val coordinates: Coordinates,
    val zone: ZoneId,
    val isDevice: Boolean,
) {
    fun name(arabic: Boolean) = if (arabic) nameAr else nameEn
}

data class PrayerSnapshot(
    val location: PrayerLocation,
    val today: DailyPrayerTimes,
    val next: PrayerMoment?,
    val previous: PrayerMoment?,
)

class PrayerRepository {
    fun location(settings: AppSettings): PrayerLocation? {
        Cities.byId(settings.cityId)?.let { city ->
            return PrayerLocation(
                nameAr = "${city.nameAr}، ${city.countryAr}",
                nameEn = "${city.nameEn}, ${city.countryEn}",
                coordinates = city.coordinates,
                zone = ZoneId.of(city.zoneId),
                isDevice = false,
            )
        }
        settings.deviceLocation?.let { d ->
            val zone = runCatching { ZoneId.of(d.zoneId) }.getOrDefault(ZoneId.systemDefault())
            val coords = "%.3f, %.3f".format(java.util.Locale.ENGLISH, d.latitude, d.longitude)
            return PrayerLocation("موقعي ($coords)", "My location ($coords)", Coordinates(d.latitude, d.longitude), zone, true)
        }
        return null
    }

    fun snapshot(settings: AppSettings, now: ZonedDateTime = ZonedDateTime.now()): PrayerSnapshot? {
        val location = location(settings) ?: return null
        val calc = PrayerTimesCalculator(settings.calculationMethod)
        val localNow = now.withZoneSameInstant(location.zone)
        return PrayerSnapshot(
            location = location,
            today = calc.calculate(localNow.toLocalDate(), location.coordinates, location.zone),
            next = calc.nextPrayer(localNow, location.coordinates, location.zone),
            previous = calc.previousPrayer(localNow, location.coordinates, location.zone),
        )
    }

    fun day(settings: AppSettings, date: LocalDate): DailyPrayerTimes? {
        val location = location(settings) ?: return null
        return PrayerTimesCalculator(settings.calculationMethod).calculate(date, location.coordinates, location.zone)
    }
}
