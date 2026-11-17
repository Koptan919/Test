package com.noor.app

import android.content.Context
import com.noor.app.data.db.NoorDatabase
import com.noor.app.data.duas.DuaRepository
import com.noor.app.data.prayer.LocationProvider
import com.noor.app.data.prayer.PrayerRepository
import com.noor.app.data.prefs.SettingsRepository
import com.noor.app.data.quran.QuranRepository
import com.noor.core.calendar.AdjustedHijriConverter
import com.noor.core.calendar.HijriConverter
import com.noor.core.calendar.UmmAlQuraConverter

/** Simple manual dependency container (no DI framework needed for an app this size). */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: NoorDatabase by lazy { NoorDatabase.create(appContext) }
    val settings: SettingsRepository by lazy { SettingsRepository(appContext) }
    val quran: QuranRepository by lazy { QuranRepository(appContext, database.readingDao()) }
    val duas: DuaRepository by lazy { DuaRepository(appContext, quran) }
    val prayer: PrayerRepository by lazy { PrayerRepository() }
    val location: LocationProvider by lazy { LocationProvider(appContext) }

    fun hijri(adjustDays: Int): HijriConverter = AdjustedHijriConverter(UmmAlQuraConverter, adjustDays)
}
