package com.noor.app

import android.content.Context
import com.noor.app.data.audio.AudioLibrary
import com.noor.app.data.db.NoorDatabase
import com.noor.app.data.duas.DuaRepository
import com.noor.app.data.prayer.LocationProvider
import com.noor.app.data.prayer.PrayerRepository
import com.noor.app.data.prefs.SettingsRepository
import com.noor.app.data.quran.QuranRepository
import com.noor.app.data.remote.Connectivity
import com.noor.app.data.remote.ContentSync
import com.noor.app.data.remote.GitHubClient
import com.noor.app.data.remote.UpdateRepository
import com.noor.core.calendar.AdjustedHijriConverter
import com.noor.core.calendar.HijriConverter
import com.noor.core.calendar.UmmAlQuraConverter
import java.io.File

/** Simple manual dependency container (no DI framework needed for an app this size). */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: NoorDatabase by lazy { NoorDatabase.create(appContext) }
    val settings: SettingsRepository by lazy { SettingsRepository(appContext) }
    val quran: QuranRepository by lazy { QuranRepository(appContext, database.readingDao()) }
    val duas: DuaRepository by lazy { DuaRepository(appContext, quran) }
    val prayer: PrayerRepository by lazy { PrayerRepository() }
    val location: LocationProvider by lazy { LocationProvider(appContext) }
    val connectivity: Connectivity by lazy { Connectivity(appContext) }
    private val github: GitHubClient by lazy { GitHubClient() }
    val updates: UpdateRepository by lazy { UpdateRepository(github, settings, connectivity) }
    val contentSync: ContentSync by lazy { ContentSync(github, duas, settings, connectivity) }
    val audio: AudioLibrary by lazy { AudioLibrary(appContext, database.audioDao()) }

    fun hijri(adjustDays: Int): HijriConverter = AdjustedHijriConverter(UmmAlQuraConverter, adjustDays)

    /** Size of disposable caches (never includes user data or downloaded content). */
    fun cacheSizeBytes(): Long = appContext.cacheDir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }

    fun clearCache() {
        appContext.cacheDir.listFiles()?.forEach(File::deleteRecursively)
    }
}
