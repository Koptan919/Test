package com.noor.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noor.app.AppContainer
import com.noor.app.data.db.ReadingProgressEntity
import com.noor.app.data.duas.DuaText
import com.noor.app.data.prayer.PrayerSnapshot
import com.noor.app.data.prefs.AppSettings
import com.noor.core.calendar.HijriDate
import com.noor.core.calendar.Occasion
import com.noor.core.calendar.Occasions
import com.noor.core.calendar.Season
import com.noor.core.calendar.Seasons
import com.noor.core.content.DuaCatalog
import com.noor.core.content.DuaEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

data class UpcomingOccasion(val occasion: Occasion, val date: LocalDate, val daysAway: Long)

data class DailyDua(val entry: DuaEntry, val text: String?)

data class HomeState(
    val settings: AppSettings,
    val today: LocalDate,
    val hijri: HijriDate,
    val season: Season,
    val prayer: PrayerSnapshot?,
    val occasionsToday: List<Occasion>,
    val upcoming: UpcomingOccasion?,
    val dailyDua: DailyDua?,
    val latestReading: ReadingProgressEntity?,
    val online: Boolean,
)

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    private val dailyDua = MutableStateFlow<DailyDua?>(null)

    /** Prayer data only needs refreshing about once a minute; the countdown ticks in the UI. */
    private val ticker = flow {
        while (true) {
            emit(ZonedDateTime.now())
            delay(30_000)
        }
    }

    init {
        viewModelScope.launch {
            val entry = DuaCatalog.duaOfTheDay(LocalDate.now())
            val text = when (val t = container.duas.text(entry, arabic = true)) {
                is DuaText.Quranic -> t.ayahs.joinToString(" ") { it.text }
                is DuaText.Lines -> t.lines.joinToString("\n")
                DuaText.Unavailable -> null
            }
            dailyDua.value = DailyDua(entry, text)
        }
    }

    private val base = combine(container.settings.settings, ticker, container.quran.latestProgress) { s, now, latest ->
        Triple(s, now, latest)
    }

    val state: StateFlow<HomeState?> = combine(
        base,
        dailyDua,
        container.connectivity.online.distinctUntilChanged(),
    ) { (settings, now, latest), dua, online ->
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
            season = if (settings.occasionThemes) Seasons.of(hijri) else Season.NONE,
            prayer = container.prayer.snapshot(settings, now),
            occasionsToday = Occasions.on(hijri, converter),
            upcoming = upcoming,
            dailyDua = dua,
            latestReading = latest,
            online = online,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
