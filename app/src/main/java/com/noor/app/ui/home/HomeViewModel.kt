package com.noor.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noor.app.AppContainer
import com.noor.app.data.db.ReadingProgressEntity
import com.noor.app.data.prayer.PrayerSnapshot
import com.noor.app.data.prefs.AppSettings
import com.noor.core.calendar.HijriDate
import com.noor.core.calendar.Occasion
import com.noor.core.calendar.Occasions
import com.noor.core.content.DailyVerses
import com.noor.core.quran.Ayah
import com.noor.core.quran.AyahRef
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

data class UpcomingOccasion(val occasion: Occasion, val date: LocalDate, val daysAway: Long)

data class HomeState(
    val settings: AppSettings,
    val today: LocalDate,
    val hijri: HijriDate,
    val prayer: PrayerSnapshot?,
    val occasionsToday: List<Occasion>,
    val upcoming: UpcomingOccasion?,
    val verseRef: AyahRef,
    val verse: Ayah?,
    val latestReading: ReadingProgressEntity?,
)

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    private val verse = MutableStateFlow<Ayah?>(null)
    private val ticker = flow {
        while (true) {
            emit(ZonedDateTime.now())
            delay(15_000)
        }
    }

    init {
        viewModelScope.launch { verse.value = container.quran.ayah(DailyVerses.forDate(LocalDate.now())) }
    }

    val state: StateFlow<HomeState?> = combine(
        container.settings.settings,
        ticker,
        container.quran.latestProgress,
        verse,
    ) { settings, now, latest, ayah ->
        val converter = container.hijri(settings.hijriAdjust)
        val today = now.toLocalDate()
        val hijri = converter.toHijri(today)
        var upcoming: UpcomingOccasion? = null
        for (offset in 1L..90L) {
            val d = today.plusDays(offset)
            val found = Occasions.on(converter.toHijri(d), converter).firstOrNull()
            if (found != null) {
                upcoming = UpcomingOccasion(found, d, ChronoUnit.DAYS.between(today, d))
                break
            }
        }
        HomeState(
            settings = settings,
            today = today,
            hijri = hijri,
            prayer = container.prayer.snapshot(settings, now),
            occasionsToday = Occasions.on(hijri, converter),
            upcoming = upcoming,
            verseRef = DailyVerses.forDate(today),
            verse = ayah,
            latestReading = latest,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
