package com.noor.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.noor.core.prayer.CalculationMethod
import com.noor.core.prayer.Prayer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "noor_settings")

enum class ThemeMode { SYSTEM, DARK, LIGHT }

/** A location detected from the device (manual selection uses [AppSettings.cityId]). */
data class DeviceLocation(val latitude: Double, val longitude: Double, val zoneId: String)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val quranFontSize: Float = DEFAULT_QURAN_FONT,
    val textScale: Float = 1f,
    val cityId: String? = null,
    val deviceLocation: DeviceLocation? = null,
    val calculationMethod: CalculationMethod = CalculationMethod.JAFARI,
    val hijriAdjust: Int = 0,
    val notificationsEnabled: Boolean = false,
    val notifyPrayers: Set<Prayer> = Prayer.prayers.toSet(),
    val lastReadSurah: Int? = null,
    val lastReadAyah: Int? = null,
) {
    val hasLocation: Boolean get() = cityId != null || deviceLocation != null

    companion object {
        const val DEFAULT_QURAN_FONT = 28f
        const val MIN_QURAN_FONT = 20f
        const val MAX_QURAN_FONT = 46f
    }
}

class SettingsRepository(private val context: Context) {
    private object Keys {
        val theme = stringPreferencesKey("theme_mode")
        val quranFont = floatPreferencesKey("quran_font_size")
        val textScale = floatPreferencesKey("text_scale")
        val cityId = stringPreferencesKey("city_id")
        val devLat = doublePreferencesKey("device_lat")
        val devLng = doublePreferencesKey("device_lng")
        val devZone = stringPreferencesKey("device_zone")
        val method = stringPreferencesKey("calc_method")
        val hijriAdjust = intPreferencesKey("hijri_adjust")
        val notifications = booleanPreferencesKey("notifications_enabled")
        val notifyPrayers = stringSetPreferencesKey("notify_prayers")
        val lastSurah = intPreferencesKey("last_read_surah")
        val lastAyah = intPreferencesKey("last_read_ayah")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        val lat = p[Keys.devLat]
        val lng = p[Keys.devLng]
        val zone = p[Keys.devZone]
        AppSettings(
            themeMode = p[Keys.theme]?.let { v -> ThemeMode.entries.firstOrNull { it.name == v } } ?: ThemeMode.DARK,
            quranFontSize = p[Keys.quranFont] ?: AppSettings.DEFAULT_QURAN_FONT,
            textScale = p[Keys.textScale] ?: 1f,
            cityId = p[Keys.cityId],
            deviceLocation = if (lat != null && lng != null && zone != null) DeviceLocation(lat, lng, zone) else null,
            calculationMethod = p[Keys.method]?.let { v -> CalculationMethod.entries.firstOrNull { it.name == v } }
                ?: CalculationMethod.JAFARI,
            hijriAdjust = p[Keys.hijriAdjust] ?: 0,
            notificationsEnabled = p[Keys.notifications] ?: false,
            notifyPrayers = p[Keys.notifyPrayers]
                ?.mapNotNull { v -> Prayer.entries.firstOrNull { it.name == v } }?.toSet()
                ?: Prayer.prayers.toSet(),
            lastReadSurah = p[Keys.lastSurah],
            lastReadAyah = p[Keys.lastAyah],
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setThemeMode(mode: ThemeMode) = context.dataStore.edit { it[Keys.theme] = mode.name }

    suspend fun setQuranFontSize(size: Float) = context.dataStore.edit {
        it[Keys.quranFont] = size.coerceIn(AppSettings.MIN_QURAN_FONT, AppSettings.MAX_QURAN_FONT)
    }

    suspend fun setTextScale(scale: Float) = context.dataStore.edit { it[Keys.textScale] = scale.coerceIn(0.85f, 1.4f) }

    suspend fun selectCity(cityId: String) = context.dataStore.edit {
        it[Keys.cityId] = cityId
        it.remove(Keys.devLat); it.remove(Keys.devLng); it.remove(Keys.devZone)
    }

    suspend fun setDeviceLocation(location: DeviceLocation) = context.dataStore.edit {
        it[Keys.devLat] = location.latitude
        it[Keys.devLng] = location.longitude
        it[Keys.devZone] = location.zoneId
        it.remove(Keys.cityId)
    }

    suspend fun setCalculationMethod(method: CalculationMethod) = context.dataStore.edit { it[Keys.method] = method.name }

    suspend fun setHijriAdjust(days: Int) = context.dataStore.edit { it[Keys.hijriAdjust] = days.coerceIn(-2, 2) }

    suspend fun setNotificationsEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.notifications] = enabled }

    suspend fun setPrayerNotification(prayer: Prayer, enabled: Boolean) = context.dataStore.edit { p ->
        val current = p[Keys.notifyPrayers] ?: Prayer.prayers.map { it.name }.toSet()
        p[Keys.notifyPrayers] = if (enabled) current + prayer.name else current - prayer.name
    }

    suspend fun setLastRead(surah: Int, ayah: Int) = context.dataStore.edit {
        it[Keys.lastSurah] = surah
        it[Keys.lastAyah] = ayah
    }
}
