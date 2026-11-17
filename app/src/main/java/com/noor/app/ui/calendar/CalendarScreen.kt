package com.noor.app.ui.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.noor.app.AppContainer
import com.noor.app.R
import com.noor.app.data.prefs.AppSettings
import com.noor.app.ui.components.GlassCard
import com.noor.app.ui.components.HeroCard
import com.noor.app.ui.components.NoorTopBar
import com.noor.app.ui.components.SectionHeader
import com.noor.app.ui.isArabic
import com.noor.app.ui.navigation.NoorNavigator
import com.noor.app.ui.noorViewModel
import com.noor.app.ui.theme.LocalNoorPalette
import com.noor.app.util.Formatters
import com.noor.core.calendar.HijriConverter
import com.noor.core.calendar.HijriDate
import com.noor.core.calendar.HijriMonths
import com.noor.core.calendar.Occasion
import com.noor.core.calendar.OccasionType
import com.noor.core.calendar.Occasions
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class CalendarViewModel(private val container: AppContainer) : ViewModel() {
    val settings: StateFlow<AppSettings?> = container.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun converter(adjust: Int): HijriConverter = container.hijri(adjust)
}

private val weekOrder = listOf(
    DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
)

@Composable
private fun occasionColor(type: OccasionType): Color {
    val palette = LocalNoorPalette.current
    return when (type) {
        OccasionType.MARTYRDOM -> Color(0xFFB0364F)
        OccasionType.BIRTH, OccasionType.EID -> palette.accent
        OccasionType.SACRED_NIGHT -> Color(0xFF7FA7D9)
        OccasionType.HISTORIC -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
fun CalendarScreen(nav: NoorNavigator) {
    val vm = noorViewModel { CalendarViewModel(it) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    val arabic = isArabic()
    val palette = LocalNoorPalette.current
    val s = settings
    if (s == null) {
        Column(Modifier.fillMaxSize()) { NoorTopBar(stringResource(R.string.module_calendar), onBack = nav::back) }
        return
    }
    val converter = vm.converter(s.hijriAdjust)
    val today = LocalDate.now()
    val todayHijri = converter.toHijri(today)
    var year by rememberSaveable { mutableIntStateOf(todayHijri.year) }
    var month by rememberSaveable { mutableIntStateOf(todayHijri.month) }
    var selectedDay by rememberSaveable { mutableStateOf<Int?>(null) }
    var direction by rememberSaveable { mutableIntStateOf(1) }

    fun shift(delta: Int) {
        direction = delta
        selectedDay = null
        var m = month + delta
        var y = year
        if (m < 1) { m = 12; y -= 1 }
        if (m > 12) { m = 1; y += 1 }
        month = m
        year = y
    }

    Column(Modifier.fillMaxSize()) {
        NoorTopBar(stringResource(R.string.module_calendar), onBack = nav::back)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            HeroCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.calendar_today), style = MaterialTheme.typography.labelLarge, color = palette.heroAccent)
                Text(
                    "${Formatters.number(todayHijri.day, arabic)} ${HijriMonths.name(todayHijri.month, arabic)} ${Formatters.number(todayHijri.year, arabic)}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.onHero,
                )
                Text(Formatters.gregorianLong(today, arabic), style = MaterialTheme.typography.bodyMedium, color = palette.onHero.copy(alpha = 0.8f))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { shift(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.action_previous_month)) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${HijriMonths.name(month, arabic)} ${Formatters.number(year, arabic)}",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    val first = converter.toGregorian(HijriDate(year, month, 1))
                    val last = first.plusDays(converter.monthLength(year, month) - 1L)
                    Text(
                        "${Formatters.gregorianShort(first, arabic)} – ${Formatters.gregorianShort(last, arabic)} ${Formatters.number(last.year, arabic)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { shift(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.action_next_month)) }
            }
            if (year != todayHijri.year || month != todayHijri.month) {
                TextButton(onClick = {
                    year = todayHijri.year
                    month = todayHijri.month
                    selectedDay = null
                }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(R.string.action_today)) }
            }

            GlassCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth()) {
                    weekOrder.forEach { d ->
                        Text(
                            d.getDisplayName(TextStyle.SHORT, if (arabic) Locale.forLanguageTag("ar") else Locale.ENGLISH),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                AnimatedContent(
                    targetState = year * 100 + month,
                    transitionSpec = {
                        val dir = direction
                        (slideInHorizontally(tween(280)) { it / 4 * dir } + fadeIn(tween(280))) togetherWith
                            (slideOutHorizontally(tween(200)) { -it / 4 * dir } + fadeOut(tween(200)))
                    },
                    label = "month",
                ) { key ->
                    MonthGrid(
                        year = key / 100,
                        month = key % 100,
                        converter = converter,
                        today = todayHijri,
                        selectedDay = selectedDay,
                        arabic = arabic,
                        onSelect = { selectedDay = it },
                    )
                }
            }

            val monthLength = converter.monthLength(year, month)
            val monthOccasions = Occasions.forMonth(month, monthLength)
            val shown = selectedDay?.let { d -> monthOccasions.filter { it.first == d } } ?: monthOccasions
            SectionHeader(
                if (selectedDay != null) {
                    stringResource(R.string.calendar_day_occasions, Formatters.number(selectedDay!!, arabic))
                } else {
                    stringResource(R.string.calendar_month_occasions)
                },
                actionLabel = if (selectedDay != null) stringResource(R.string.filter_all) else null,
                onAction = { selectedDay = null },
            )
            if (shown.isEmpty()) {
                Text(stringResource(R.string.calendar_no_occasions), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
            shown.forEach { (day, occasion) -> OccasionRow(day, occasion, year, month, converter, arabic) }

            GlassCard(Modifier.fillMaxWidth()) {
                Row {
                    Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.calendar_notice), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MonthGrid(
    year: Int,
    month: Int,
    converter: HijriConverter,
    today: HijriDate,
    selectedDay: Int?,
    arabic: Boolean,
    onSelect: (Int) -> Unit,
) {
    val palette = LocalNoorPalette.current
    val length = converter.monthLength(year, month)
    val first = converter.toGregorian(HijriDate(year, month, 1))
    val offset = weekOrder.indexOf(first.dayOfWeek)
    val occasionsByDay = Occasions.forMonth(month, length).groupBy({ it.first }, { it.second })
    val cells = offset + length
    val rows = (cells + 6) / 7
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (c in 0 until 7) {
                    val day = r * 7 + c - offset + 1
                    Box(Modifier.weight(1f).aspectRatio(0.82f)) {
                        if (day in 1..length) {
                            val isToday = today.year == year && today.month == month && today.day == day
                            val isSelected = selectedDay == day
                            val occ = occasionsByDay[day].orEmpty()
                            val g = first.plusDays(day - 1L)
                            Column(
                                Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when {
                                            isToday -> palette.accent.copy(alpha = 0.22f)
                                            isSelected -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                            else -> Color.Transparent
                                        },
                                    )
                                    .then(if (isSelected) Modifier.border(1.dp, palette.accent, RoundedCornerShape(12.dp)) else Modifier)
                                    .clickable { onSelect(day) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    Formatters.number(day, arabic),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isToday) palette.accent else MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    Formatters.number(g.dayOfMonth, arabic),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    occ.take(3).forEach {
                                        Box(Modifier.size(5.dp).clip(CircleShape).background(occasionColor(it.type)))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OccasionRow(day: Int, occasion: Occasion, year: Int, month: Int, converter: HijriConverter, arabic: Boolean) {
    val date = converter.toGregorian(HijriDate(year, month, day))
    GlassCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(occasionColor(occasion.type).copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(Formatters.number(day, arabic), style = MaterialTheme.typography.titleSmall, color = occasionColor(occasion.type))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(occasion.title(arabic), style = MaterialTheme.typography.titleSmall)
                Text(
                    Formatters.weekdayShort(date, arabic) + " · " + Formatters.gregorianShort(date, arabic) + " " + Formatters.number(date.year, arabic),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
