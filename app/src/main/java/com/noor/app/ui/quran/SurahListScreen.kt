package com.noor.app.ui.quran

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noor.app.R
import com.noor.app.data.db.fraction
import com.noor.app.ui.components.AyahNumber
import com.noor.app.ui.components.GlassCard
import com.noor.app.ui.components.HeroCard
import com.noor.app.ui.components.NoorTopBar
import com.noor.app.ui.isArabic
import com.noor.app.ui.navigation.NoorNavigator
import com.noor.app.ui.navigation.Routes
import com.noor.app.ui.noorViewModel
import com.noor.app.ui.theme.LocalNoorPalette
import com.noor.app.ui.theme.NoorFonts
import com.noor.app.util.Formatters
import com.noor.core.quran.QuranMeta

@Composable
fun SurahListScreen(nav: NoorNavigator) {
    val vm = noorViewModel { SurahListViewModel(it) }
    val progress by vm.progress.collectAsStateWithLifecycle()
    val latest by vm.latest.collectAsStateWithLifecycle()
    val arabic = isArabic()
    val palette = LocalNoorPalette.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        NoorTopBar(stringResource(R.string.module_quran), onBack = nav::back)
        TabRow(
            selectedTabIndex = tab,
            containerColor = Color.Transparent,
            contentColor = palette.accent,
            indicator = { positions ->
                TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(positions[tab]), color = palette.accent)
            },
        ) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.quran_tab_surahs)) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.quran_tab_juz)) })
        }
        LazyColumn(
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            latest?.let { l ->
                item {
                    val meta = QuranMeta.surah(l.surah)
                    HeroCard(Modifier.fillMaxWidth(), onClick = { nav.open(Routes.reader(l.surah, l.ayah)) }) {
                        Text(stringResource(R.string.home_continue_reading), style = MaterialTheme.typography.labelLarge, color = palette.heroAccent)
                        Text(
                            if (arabic) "سورة ${meta.nameArabic}" else meta.nameTransliterated,
                            style = MaterialTheme.typography.headlineSmall,
                            color = palette.onHero,
                        )
                        Text(
                            stringResource(R.string.quran_ayah_of, Formatters.number(l.ayah, arabic), Formatters.number(l.ayahCount, arabic)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.onHero.copy(alpha = 0.8f),
                        )
                    }
                }
            }
            if (tab == 0) {
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.quran_search_hint)) },
                        leadingIcon = { Icon(Icons.Outlined.Search, null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, stringResource(R.string.action_clear)) }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.accent,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                    )
                }
                val q = query.trim().lowercase()
                val surahs = QuranMeta.surahs.filter {
                    q.isEmpty() || it.nameArabic.contains(q) || it.nameTransliterated.lowercase().contains(q) ||
                        it.nameEnglish.lowercase().contains(q) || it.number.toString() == q
                }
                items(surahs, key = { it.number }) { s ->
                    val p = progress[s.number]
                    GlassCard(Modifier.fillMaxWidth(), onClick = { nav.open(Routes.reader(s.number)) }, contentPadding = PaddingValues(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AyahNumber(Formatters.number(s.number, arabic), size = 40.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(if (arabic) s.nameArabic else s.nameTransliterated, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    stringResource(
                                        R.string.quran_surah_info,
                                        stringResource(if (s.isMeccan) R.string.quran_meccan else R.string.quran_medinan),
                                        Formatters.number(s.ayahCount, arabic),
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                s.nameArabic,
                                style = MaterialTheme.typography.titleLarge.copy(fontFamily = NoorFonts.Quran, fontSize = 22.sp),
                                color = palette.accent,
                            )
                        }
                        if (p != null) {
                            Spacer(Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { p.fraction },
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                                color = palette.accent,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            )
                        }
                    }
                }
            } else {
                items(QuranMeta.juzStarts.size) { index ->
                    val (surah, ayah) = QuranMeta.juzStarts[index]
                    val meta = QuranMeta.surah(surah)
                    GlassCard(Modifier.fillMaxWidth(), onClick = { nav.open(Routes.reader(surah, ayah)) }, contentPadding = PaddingValues(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AyahNumber(Formatters.number(index + 1, arabic), size = 40.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.quran_juz_n, Formatters.number(index + 1, arabic)), style = MaterialTheme.typography.titleSmall)
                                Text(
                                    stringResource(
                                        R.string.quran_juz_starts,
                                        if (arabic) meta.nameArabic else meta.nameTransliterated,
                                        Formatters.number(ayah, arabic),
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(Icons.Outlined.AutoStories, null, tint = palette.accent)
                        }
                    }
                }
            }
        }
    }
}
