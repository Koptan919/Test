package com.noor.app.ui.home

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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.TouchApp
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
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
import com.noor.app.ui.appContainer
import com.noor.app.ui.components.GlassCard
import com.noor.app.ui.components.HeroCard
import com.noor.app.ui.components.IconBadge
import com.noor.app.ui.components.LoadingList
import com.noor.app.ui.components.OfflineChip
import com.noor.app.ui.components.ProgressRing
import com.noor.app.ui.components.SectionHeader
import com.noor.app.ui.isArabic
import com.noor.app.ui.navigation.NoorNavigator
import com.noor.app.ui.navigation.Routes
import com.noor.app.ui.noorViewModel
import com.noor.app.ui.prayerIcon
import com.noor.app.ui.prayerNameRes
import com.noor.app.ui.theme.LocalNoorPalette
import com.noor.app.ui.theme.LocalReduceMotion
import com.noor.app.ui.theme.NoorFonts
import com.noor.app.util.Formatters
import com.noor.core.calendar.HijriMonths
import com.noor.core.calendar.Season
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

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "header") { Header(s, arabic, nav) }
        if (s.season != Season.NONE) item(key = "season") { SeasonBanner(s.season, nav) }
        item(key = "hero") { PrayerHero(s.prayer, arabic, onOpen = { nav.open(Routes.PRAYER) }, onSetCity = { nav.open(Routes.CITY) }) }
        item(key = "quick") { QuickAccess(s, arabic, nav) }
        item(key = "daily") { SectionHeader(stringResource(R.string.home_daily)) }
        item(key = "dua") { DuaOfTheDay(s, arabic, nav) }
        item(key = "occasion") { OccasionCard(s, arabic, nav) }
        s.latestReading?.let { latest ->
            item(key = "continue") {
                val meta = QuranMeta.surah(latest.surah)
                GlassCard(
                    Modifier.fillMaxWidth(),
                    onClick = { nav.open(Routes.reader(latest.surah, latest.ayah)) },
                    onClickLabel = stringResource(R.string.home_continue_reading),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Outlined.AutoStories)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.home_continue_reading), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (arabic) "سورة ${meta.nameArabic}" else "${meta.nameTransliterated} · ${meta.nameArabic}", style = MaterialTheme.typography.titleMedium)
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
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (s.settings.recents.isNotEmpty()) {
            item(key = "recents") { Recents(s.settings.recents, arabic, nav) }
        }
        item(key = "tools") { Tools(nav) }
    }
}

@Composable
private fun Header(s: HomeState, arabic: Boolean, nav: NoorNavigator) {
    Column(Modifier.statusBarsPadding().padding(top = 14.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.greeting_salam),
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = NoorFonts.Naskh, fontSize = 28.sp, lineHeight = 40.sp),
                    color = LocalNoorPalette.current.accent,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    "${Formatters.number(s.hijri.day, arabic)} ${HijriMonths.name(s.hijri.month, arabic)} ${Formatters.number(s.hijri.year, arabic)} ${stringResource(R.string.hijri_suffix)}",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(Formatters.gregorianLong(s.today, arabic), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { nav.switchTab(Routes.SETTINGS) }) {
                Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.nav_settings), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        AnimatedVisibility(visible = !s.online) { OfflineChip(Modifier.padding(top = 10.dp)) }
    }
}

@Composable
private fun SeasonBanner(season: Season, nav: NoorNavigator) {
    val palette = LocalNoorPalette.current
    val (title, body, links) = when (season) {
        Season.RAMADAN -> Triple(R.string.season_ramadan, R.string.season_ramadan_body, listOf(R.string.module_quran to Routes.QURAN, R.string.season_link_iftitah to Routes.dua("iftitah")))
        Season.MUHARRAM -> Triple(R.string.season_muharram, R.string.season_muharram_body, listOf(R.string.season_link_ashura to Routes.dua("ashura"), R.string.season_link_warith to Routes.dua("warith")))
        Season.ARBAEEN -> Triple(R.string.season_arbaeen, R.string.season_arbaeen_body, listOf(R.string.season_link_arbaeen to Routes.dua("arbaeen")))
        Season.NONE -> return
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, palette.heroAccent.copy(alpha = 0.3f), RoundedCornerShape(22.dp))
            .background(palette.heroStart.copy(alpha = 0.55f))
            .padding(16.dp),
    ) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = palette.heroAccent, modifier = Modifier.semantics { heading() })
        Text(stringResource(body), style = MaterialTheme.typography.bodySmall, color = palette.onHero.copy(alpha = 0.85f))
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            links.forEach { (label, route) ->
                com.noor.app.ui.components.AccentButton(stringResource(label), { nav.open(route) })
            }
        }
    }
}

@Composable
private fun PrayerHero(snapshot: PrayerSnapshot?, arabic: Boolean, onOpen: () -> Unit, onSetCity: () -> Unit) {
    val palette = LocalNoorPalette.current
    if (snapshot == null || snapshot.next == null) {
        HeroCard(Modifier.fillMaxWidth(), onClick = onSetCity) {
            Text(stringResource(R.string.prayer_no_location_title), style = MaterialTheme.typography.titleLarge, color = palette.onHero)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.prayer_no_location_msg), style = MaterialTheme.typography.bodyMedium, color = palette.onHero.copy(alpha = 0.85f))
            Spacer(Modifier.height(14.dp))
            com.noor.app.ui.components.AccentButton(stringResource(R.string.prayer_choose_city), onSetCity, icon = Icons.Outlined.LocationOn)
        }
        return
    }
    val next = snapshot.next!!
    HeroCard(Modifier.fillMaxWidth(), onClick = onOpen, onClickLabel = stringResource(R.string.module_prayer)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                LiveDot()
                Text(
                    stringResource(prayerNameRes(next.prayer)),
                    style = MaterialTheme.typography.displaySmall,
                    color = palette.onHero,
                )
                Text(Formatters.time(next.time, arabic), style = MaterialTheme.typography.titleMedium, color = palette.onHero.copy(alpha = 0.85f))
            }
            LiveCountdown(snapshot, arabic)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.LocationOn, null, tint = palette.onHero.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(snapshot.location.name(arabic), style = MaterialTheme.typography.bodySmall, color = palette.onHero.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(14.dp))
        // Today's five prayers inside the hero, so the dashboard isn't a stack of identical cards.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Prayer.prayers.forEach { p ->
                val t = snapshot.today[p]
                val isNext = next.prayer == p && next.time.toLocalDate() == snapshot.today.date
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isNext) palette.heroAccent.copy(alpha = 0.16f) else androidx.compose.ui.graphics.Color.Transparent)
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(prayerNameRes(p)), style = MaterialTheme.typography.labelMedium, color = if (isNext) palette.heroAccent else palette.onHero.copy(alpha = 0.8f), maxLines = 1)
                    Text(t?.let { Formatters.time(it, arabic) } ?: "—", style = MaterialTheme.typography.labelSmall, color = palette.onHero, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun LiveDot() {
    val palette = LocalNoorPalette.current
    val alpha = if (LocalReduceMotion.current) 1f else rememberInfiniteTransition(label = "pulse")
        .animateFloat(0.35f, 1f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "dot").value
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).alpha(alpha).clip(CircleShape).background(palette.heroAccent))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.home_next_prayer), style = MaterialTheme.typography.labelLarge, color = palette.heroAccent)
    }
}

/** The only part of the home screen that recomposes every second. */
@Composable
private fun LiveCountdown(snapshot: PrayerSnapshot, arabic: Boolean) {
    val palette = LocalNoorPalette.current
    val next = snapshot.next ?: return
    val prev = snapshot.previous
    val now by produceState(ZonedDateTime.now()) {
        while (true) {
            delay(1_000L - System.currentTimeMillis() % 1_000L)
            value = ZonedDateTime.now()
        }
    }
    val remaining = Duration.between(now, next.time)
    val progress = if (prev != null) {
        val total = Duration.between(prev.time, next.time).seconds.toFloat()
        if (total > 0) 1f - remaining.seconds / total else 0f
    } else 0f
    val label = Formatters.countdown(remaining, arabic)
    val a11y = stringResource(R.string.home_time_remaining) + " " + label
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.semantics {
            contentDescription = a11y
            liveRegion = LiveRegionMode.Polite
        },
    ) {
        ProgressRing(progress, Modifier.size(118.dp), palette.heroAccent, palette.onHero.copy(alpha = 0.14f), strokeWidth = 5.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.home_time_remaining), style = MaterialTheme.typography.labelSmall, color = palette.onHero.copy(alpha = 0.7f))
            Countdown(label)
        }
    }
}

/** Per-digit rolling animation; always laid out left-to-right. */
@Composable
private fun Countdown(text: String) {
    val palette = LocalNoorPalette.current
    val reduce = LocalReduceMotion.current
    val style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        if (reduce) {
            Text(text, style = style, color = palette.onHero)
            return@CompositionLocalProvider
        }
        Row {
            text.forEachIndexed { index, c ->
                AnimatedContent(
                    targetState = c,
                    transitionSpec = { (slideInVertically { -it / 2 } + fadeIn()) togetherWith (slideOutVertically { it / 2 } + fadeOut()) },
                    label = "digit$index",
                ) { ch -> Text(ch.toString(), style = style, color = palette.onHero) }
            }
        }
    }
}

/** Asymmetric quick access: a large Quran tile beside two compact tiles, then the library strip. */
@Composable
private fun QuickAccess(s: HomeState, arabic: Boolean, nav: NoorNavigator) {
    val palette = LocalNoorPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassCard(
                Modifier.weight(1.1f).fillMaxHeight(),
                onClick = { nav.open(Routes.QURAN) },
                contentPadding = PaddingValues(18.dp),
            ) {
                IconBadge(Icons.Outlined.AutoStories, size = 52.dp)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.module_quran), style = MaterialTheme.typography.titleLarge)
                Text("القرآن الكريم", style = MaterialTheme.typography.bodyLarge.copy(fontFamily = NoorFonts.Quran), color = palette.accent)
                Text(stringResource(R.string.module_quran_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CompactTile(Icons.Outlined.Book, stringResource(R.string.module_duas)) { nav.open(Routes.duas()) }
                CompactTile(Icons.Outlined.AccessTime, stringResource(R.string.module_prayer)) { nav.open(Routes.PRAYER) }
            }
        }
        GlassCard(Modifier.fillMaxWidth(), onClick = { nav.open(Routes.LIBRARY) }, contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Outlined.LocalLibrary, size = 40.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.module_library), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.library_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CompactTile(icon: ImageVector, label: String, onClick: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth().heightIn(min = 76.dp), onClick = onClick, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, size = 38.dp)
            Spacer(Modifier.width(10.dp))
            Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DuaOfTheDay(s: HomeState, arabic: Boolean, nav: NoorNavigator) {
    val palette = LocalNoorPalette.current
    val dua = s.dailyDua ?: return
    GlassCard(Modifier.fillMaxWidth(), onClick = { nav.open(Routes.dua(dua.entry.id)) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.home_dua_of_day), style = MaterialTheme.typography.labelLarge, color = palette.accent, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.duas_verified_text), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(6.dp))
        Text(dua.entry.title(arabic), style = MaterialTheme.typography.titleMedium)
        if (dua.text != null) {
            Spacer(Modifier.height(8.dp))
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    dua.text,
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = NoorFonts.Quran, fontSize = 22.sp, lineHeight = 44.sp),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        val ref = dua.entry.quranRefs.firstOrNull()
        val refText = if (ref != null) {
            val meta = QuranMeta.surah(ref.surah)
            if (arabic) "سورة ${meta.nameArabic} · ${Formatters.number(ref.ayah, true)}" else "${meta.nameTransliterated} ${ref.surah}:${ref.ayah}"
        } else dua.entry.source(arabic)
        Text(stringResource(R.string.duas_source, refText), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OccasionCard(s: HomeState, arabic: Boolean, nav: NoorNavigator) {
    val palette = LocalNoorPalette.current
    val today = s.occasionsToday.firstOrNull()
    val up = s.upcoming
    if (today == null && up == null) return
    GlassCard(Modifier.fillMaxWidth(), onClick = { nav.open(Routes.CALENDAR) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.heroStart),
                contentAlignment = Alignment.Center,
            ) {
                if (today != null) {
                    Icon(Icons.Outlined.Star, null, tint = palette.heroAccent)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(Formatters.number(up!!.daysAway, arabic), style = MaterialTheme.typography.titleLarge, color = palette.heroAccent)
                        Text(stringResource(R.string.home_days_short), style = MaterialTheme.typography.labelSmall, color = palette.onHero.copy(alpha = 0.8f))
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (today != null) R.string.home_today_occasion else R.string.home_upcoming),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text((today ?: up!!.occasion).title(arabic), style = MaterialTheme.typography.titleSmall)
                if (today == null) {
                    Text(Formatters.weekdayShort(up!!.date, arabic) + " · " + Formatters.gregorianShort(up.date, arabic), style = MaterialTheme.typography.bodySmall, color = palette.accent)
                }
            }
        }
    }
}

@Composable
private fun Recents(recents: List<String>, arabic: Boolean, nav: NoorNavigator) {
    val container = appContainer()
    val items = recents.mapNotNull { key ->
        val (kind, id) = key.split(':', limit = 2).let { if (it.size == 2) it[0] to it[1] else return@mapNotNull null }
        when (kind) {
            "surah" -> id.toIntOrNull()?.takeIf { it in 1..114 }?.let { n ->
                val m = QuranMeta.surah(n)
                Triple(if (arabic) "سورة ${m.nameArabic}" else m.nameTransliterated, Icons.Outlined.AutoStories, Routes.reader(n))
            }
            "dua" -> container.duas.byId(id)?.let { Triple(it.title(arabic), Icons.Outlined.Book, Routes.dua(id)) }
            else -> null
        }
    }
    if (items.isEmpty()) return
    Column {
        SectionHeader(stringResource(R.string.home_recent))
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items, key = { it.third }) { (label, icon, route) ->
                Row(
                    Modifier
                        .heightIn(min = 48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { nav.open(route) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icon, null, Modifier.size(18.dp), tint = LocalNoorPalette.current.accent)
                    Spacer(Modifier.width(8.dp))
                    Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun Tools(nav: NoorNavigator) {
    val tools = listOf(
        Triple(Icons.Outlined.Explore, R.string.module_qibla, com.noor.app.ui.navigation.Routes.QIBLA),
        Triple(Icons.Outlined.TouchApp, R.string.module_tasbih, Routes.TASBIH),
        Triple(Icons.Outlined.Event, R.string.module_calendar, Routes.CALENDAR),
        Triple(Icons.Outlined.Headphones, R.string.module_audio, Routes.AUDIO),
    )
    Column {
        SectionHeader(stringResource(R.string.home_tools))
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            tools.forEach { (icon, label, route) ->
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { nav.open(route) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    IconBadge(icon, size = 52.dp)
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(label), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 1)
                }
            }
        }
    }
}

