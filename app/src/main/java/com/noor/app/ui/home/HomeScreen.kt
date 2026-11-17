package com.noor.app.ui.home

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noor.app.R
import com.noor.app.data.db.fraction
import com.noor.app.data.prayer.PrayerSnapshot
import com.noor.app.ui.components.GlassCard
import com.noor.app.ui.components.HeroCard
import com.noor.app.ui.components.IconBadge
import com.noor.app.ui.components.LoadingList
import com.noor.app.ui.components.ProgressRing
import com.noor.app.ui.components.SectionHeader
import com.noor.app.ui.components.ShortcutTile
import com.noor.app.ui.isArabic
import com.noor.app.ui.navigation.NoorNavigator
import com.noor.app.ui.navigation.Routes
import com.noor.app.ui.noorViewModel
import com.noor.app.ui.prayerIcon
import com.noor.app.ui.prayerNameRes
import com.noor.app.ui.theme.LocalNoorPalette
import com.noor.app.ui.theme.NoorFonts
import com.noor.app.util.Formatters
import com.noor.core.calendar.HijriMonths
import com.noor.core.prayer.Prayer
import com.noor.core.quran.QuranMeta
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.ZonedDateTime

@Composable
fun HomeScreen(nav: NoorNavigator) {
    val vm = noorViewModel { HomeViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val s = state
    if (s == null) {
        LoadingList(Modifier.statusBarsPadding())
        return
    }
    val arabic = isArabic()
    val now by produceState(ZonedDateTime.now()) {
        while (true) {
            delay(1_000)
            value = ZonedDateTime.now()
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 18.dp, end = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(Modifier.statusBarsPadding().padding(top = 18.dp)) {
                val hour = now.hour
                Text(
                    stringResource(R.string.greeting_salam),
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = NoorFonts.Naskh, fontSize = 26.sp),
                    color = LocalNoorPalette.current.accent,
                )
                Text(
                    stringResource(
                        when (hour) {
                            in 4..11 -> R.string.greeting_morning
                            in 12..16 -> R.string.greeting_afternoon
                            else -> R.string.greeting_evening
                        },
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.DateRange, null, tint = LocalNoorPalette.current.accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            "${Formatters.number(s.hijri.day, arabic)} ${HijriMonths.name(s.hijri.month, arabic)} ${Formatters.number(s.hijri.year, arabic)} ${stringResource(R.string.hijri_suffix)}",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            Formatters.gregorianLong(s.today, arabic),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item { NextPrayerCard(s.prayer, now, arabic, onOpen = { nav.open(Routes.PRAYER) }, onSetCity = { nav.open(Routes.CITY) }) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ShortcutTile(Icons.Outlined.AutoStories, stringResource(R.string.module_quran), Modifier.weight(1f), stringResource(R.string.module_quran_sub)) { nav.open(Routes.QURAN) }
                ShortcutTile(Icons.Outlined.Book, stringResource(R.string.module_duas), Modifier.weight(1f), stringResource(R.string.module_duas_sub)) { nav.open(Routes.duas()) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ShortcutTile(Icons.Outlined.TouchApp, stringResource(R.string.module_tasbih), Modifier.weight(1f)) { nav.open(Routes.TASBIH) }
                ShortcutTile(Icons.Outlined.Event, stringResource(R.string.module_calendar), Modifier.weight(1f)) { nav.open(Routes.CALENDAR) }
                ShortcutTile(Icons.Outlined.LocalLibrary, stringResource(R.string.module_library), Modifier.weight(1f)) { nav.switchTab(Routes.EXPLORE) }
            }
        }

        item { ContinueReadingCard(s, arabic, nav) }

        if (s.prayer != null) {
            item {
                SectionHeader(stringResource(R.string.home_prayer_times), actionLabel = stringResource(R.string.action_view_all), onAction = { nav.open(Routes.PRAYER) })
                Spacer(Modifier.height(8.dp))
                PrayerStrip(s.prayer, arabic) { nav.open(Routes.PRAYER) }
            }
        }

        item { SectionHeader(stringResource(R.string.home_daily)) }
        if (s.occasionsToday.isNotEmpty()) {
            item {
                HeroCard(onClick = { nav.open(Routes.CALENDAR) }) {
                    Text(stringResource(R.string.home_today_occasion), style = MaterialTheme.typography.labelLarge, color = LocalNoorPalette.current.heroAccent)
                    s.occasionsToday.forEach {
                        Text(it.title(arabic), style = MaterialTheme.typography.titleLarge, color = LocalNoorPalette.current.onHero)
                    }
                }
            }
        }
        item { VerseOfTheDay(s, arabic, nav) }
        s.upcoming?.let { up ->
            item {
                GlassCard(Modifier.fillMaxWidth(), onClick = { nav.open(Routes.CALENDAR) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Outlined.Star)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.home_upcoming), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(up.occasion.title(arabic), style = MaterialTheme.typography.titleSmall)
                            Text(
                                stringResource(R.string.home_in_days, Formatters.number(up.daysAway, arabic)) + " · " + Formatters.gregorianShort(up.date, arabic),
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalNoorPalette.current.accent,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NextPrayerCard(
    snapshot: PrayerSnapshot?,
    now: ZonedDateTime,
    arabic: Boolean,
    onOpen: () -> Unit,
    onSetCity: () -> Unit,
) {
    val palette = LocalNoorPalette.current
    if (snapshot == null || snapshot.next == null) {
        HeroCard(Modifier.fillMaxWidth(), onClick = onSetCity) {
            Text(stringResource(R.string.prayer_no_location_title), style = MaterialTheme.typography.titleLarge, color = palette.onHero)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.prayer_no_location_msg), style = MaterialTheme.typography.bodyMedium, color = palette.onHero.copy(alpha = 0.8f))
            Spacer(Modifier.height(14.dp))
            FilledTonalButton(onClick = onSetCity) {
                Icon(Icons.Outlined.LocationOn, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.prayer_choose_city))
            }
        }
        return
    }
    val next = snapshot.next!!
    val prev = snapshot.previous
    val remaining = Duration.between(now, next.time)
    val progress = if (prev != null) {
        val total = Duration.between(prev.time, next.time).seconds.toFloat()
        if (total > 0) 1f - remaining.seconds / total else 0f
    } else 0f

    val pulse = rememberInfiniteTransition(label = "pulse")
    val dotAlpha by pulse.animateFloat(0.3f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "dot")

    HeroCard(Modifier.fillMaxWidth(), onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .alpha(dotAlpha)
                            .clip(CircleShape)
                            .background(palette.heroAccent),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.home_next_prayer), style = MaterialTheme.typography.labelLarge, color = palette.heroAccent)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(prayerNameRes(next.prayer)),
                    style = MaterialTheme.typography.displaySmall,
                    color = palette.onHero,
                )
                Text(
                    Formatters.time(next.time, arabic),
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.onHero.copy(alpha = 0.85f),
                )
            }
            Box(contentAlignment = Alignment.Center) {
                ProgressRing(
                    progress = progress,
                    modifier = Modifier.size(96.dp),
                    color = palette.heroAccent,
                    trackColor = palette.onHero.copy(alpha = 0.15f),
                )
                Icon(prayerIcon(next.prayer), null, tint = palette.heroAccent, modifier = Modifier.size(34.dp))
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.home_time_remaining), style = MaterialTheme.typography.labelMedium, color = palette.onHero.copy(alpha = 0.7f))
        Countdown(Formatters.countdown(remaining, arabic))
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.LocationOn, null, tint = palette.onHero.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                snapshot.location.name(arabic),
                style = MaterialTheme.typography.bodySmall,
                color = palette.onHero.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Countdown with per-digit rolling animation. Always laid out left-to-right. */
@Composable
private fun Countdown(text: String) {
    val palette = LocalNoorPalette.current
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row {
            text.forEachIndexed { index, c ->
                AnimatedContent(
                    targetState = c,
                    transitionSpec = {
                        (slideInVertically { -it / 2 } + fadeIn()) togetherWith (slideOutVertically { it / 2 } + fadeOut())
                    },
                    label = "digit$index",
                ) { ch ->
                    Text(
                        ch.toString(),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = palette.onHero,
                    )
                }
            }
        }
    }
}

@Composable
private fun PrayerStrip(snapshot: PrayerSnapshot, arabic: Boolean, onClick: () -> Unit) {
    val palette = LocalNoorPalette.current
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(Prayer.prayers) { p ->
            val time = snapshot.today[p]
            val isNext = snapshot.next?.prayer == p && snapshot.next?.time?.toLocalDate() == snapshot.today.date
            GlassCard(
                Modifier.width(96.dp),
                onClick = onClick,
                shape = RoundedCornerShape(20.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            ) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(prayerIcon(p), null, tint = if (isNext) palette.accent else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(prayerNameRes(p)), style = MaterialTheme.typography.labelLarge, color = if (isNext) palette.accent else MaterialTheme.colorScheme.onSurface)
                    Text(
                        time?.let { Formatters.time(it, arabic) } ?: "—",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun ContinueReadingCard(s: HomeState, arabic: Boolean, nav: NoorNavigator) {
    val latest = s.latestReading
    GlassCard(
        Modifier.fillMaxWidth(),
        onClick = { if (latest != null) nav.open(Routes.reader(latest.surah, latest.ayah)) else nav.open(Routes.reader(1)) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Outlined.AutoStories)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (latest != null) R.string.home_continue_reading else R.string.home_start_reading),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (latest != null) {
                    val meta = QuranMeta.surah(latest.surah)
                    Text(
                        if (arabic) "سورة ${meta.nameArabic}" else "${meta.nameTransliterated} (${meta.nameArabic})",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(R.string.quran_ayah_of, Formatters.number(latest.ayah, arabic), Formatters.number(latest.ayahCount, arabic)),
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalNoorPalette.current.accent,
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { latest.fraction },
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                        color = LocalNoorPalette.current.accent,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    )
                } else {
                    Text(stringResource(R.string.home_start_reading_sub), style = MaterialTheme.typography.titleSmall)
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun VerseOfTheDay(s: HomeState, arabic: Boolean, nav: NoorNavigator) {
    val context = LocalContext.current
    val meta = QuranMeta.surah(s.verseRef.surah)
    val reference = if (arabic) {
        "سورة ${meta.nameArabic} · ${Formatters.number(s.verseRef.ayah, true)}"
    } else {
        "${meta.nameTransliterated} ${s.verseRef.surah}:${s.verseRef.ayah}"
    }
    GlassCard(Modifier.fillMaxWidth(), onClick = { nav.open(Routes.reader(s.verseRef.surah, s.verseRef.ayah)) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.home_verse_of_day),
                style = MaterialTheme.typography.labelLarge,
                color = LocalNoorPalette.current.accent,
                modifier = Modifier.weight(1f),
            )
            val verse = s.verse
            if (verse != null) {
                IconButton(onClick = {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "${verse.text}\n\n— $reference")
                    }
                    context.startActivity(Intent.createChooser(share, null))
                }) { Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.action_share)) }
            }
        }
        AnimatedVisibility(visible = s.verse != null, enter = fadeIn(tween(600))) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    s.verse?.text.orEmpty(),
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = NoorFonts.Quran, fontSize = 24.sp, lineHeight = 48.sp),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start,
                )
            }
        }
        if (s.verse == null) {
            Text(stringResource(R.string.quran_unavailable_short), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(6.dp))
        Text(reference, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
