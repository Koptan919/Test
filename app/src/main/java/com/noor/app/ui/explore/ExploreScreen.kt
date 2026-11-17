package com.noor.app.ui.explore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.noor.app.R
import com.noor.app.ui.components.GlassCard
import com.noor.app.ui.components.IconBadge
import com.noor.app.ui.components.NoorTopBar
import com.noor.app.ui.components.SectionHeader
import com.noor.app.ui.components.ShortcutTile
import com.noor.app.ui.isArabic
import com.noor.app.ui.navigation.NoorNavigator
import com.noor.app.ui.navigation.Routes
import com.noor.app.ui.theme.LocalNoorPalette
import com.noor.app.util.Formatters
import com.noor.core.content.DuaCatalog
import com.noor.core.content.DuaCategory
import com.noor.core.quran.QuranMeta

private data class Module(val icon: ImageVector, val title: Int, val subtitle: Int, val route: String)

private val modules = listOf(
    Module(Icons.Outlined.AutoStories, R.string.module_quran, R.string.module_quran_sub, Routes.QURAN),
    Module(Icons.Outlined.Book, R.string.category_supplications, R.string.module_duas_sub, Routes.duas(DuaCategory.SUPPLICATIONS.key)),
    Module(Icons.Outlined.Place, R.string.category_ziyarat, R.string.module_ziyarat_sub, Routes.duas(DuaCategory.ZIYARAT.key)),
    Module(Icons.Outlined.NightsStay, R.string.category_munajat, R.string.module_munajat_sub, Routes.duas(DuaCategory.MUNAJAT.key)),
    Module(Icons.Outlined.Spa, R.string.category_quranic, R.string.module_quranic_sub, Routes.duas(DuaCategory.QURANIC.key)),
    Module(Icons.Outlined.TouchApp, R.string.module_tasbih, R.string.module_tasbih_sub, Routes.TASBIH),
    Module(Icons.Outlined.AccessTime, R.string.module_prayer, R.string.module_prayer_sub, Routes.PRAYER),
    Module(Icons.Outlined.Event, R.string.module_calendar, R.string.module_calendar_sub, Routes.CALENDAR),
)

private data class SearchResult(val title: String, val subtitle: String, val route: String, val icon: ImageVector)

@Composable
fun ExploreScreen(nav: NoorNavigator) {
    val arabic = isArabic()
    var query by rememberSaveable { mutableStateOf("") }
    val q = query.trim().lowercase()
    val results: List<SearchResult> = if (q.isEmpty()) emptyList() else buildList {
        QuranMeta.surahs.filter {
            it.nameArabic.contains(q) || it.nameTransliterated.lowercase().contains(q) ||
                it.nameEnglish.lowercase().contains(q) || it.number.toString() == q
        }.forEach {
            add(
                SearchResult(
                    if (arabic) "سورة ${it.nameArabic}" else "${it.number}. ${it.nameTransliterated}",
                    if (arabic) it.nameTransliterated else it.nameEnglish,
                    Routes.reader(it.number),
                    Icons.Outlined.AutoStories,
                ),
            )
        }
        DuaCatalog.entries.filter { it.titleAr.contains(q) || it.titleEn.lowercase().contains(q) }.forEach {
            add(SearchResult(it.title(arabic), it.source(arabic), Routes.dua(it.id), Icons.Outlined.Book))
        }
    }

    Column(Modifier.fillMaxSize()) {
        NoorTopBar(stringResource(R.string.nav_explore))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            placeholder = { Text(stringResource(R.string.explore_search_hint)) },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, stringResource(R.string.action_clear)) }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LocalNoorPalette.current.accent,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 156.dp),
            contentPadding = PaddingValues(18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (q.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader(stringResource(R.string.explore_results, Formatters.number(results.size, arabic)))
                }
                if (results.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            stringResource(R.string.explore_no_results),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
                items(results, span = { GridItemSpan(maxLineSpan) }) { r ->
                    GlassCard(Modifier.fillMaxWidth(), onClick = { nav.open(r.route) }, contentPadding = PaddingValues(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(r.icon, size = 38.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(r.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(r.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Outlined.Explore)
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(stringResource(R.string.module_library), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    stringResource(
                                        R.string.explore_library_summary,
                                        Formatters.number(QuranMeta.SURAH_COUNT, arabic),
                                        Formatters.number(DuaCatalog.entries.size, arabic),
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                items(modules) { m ->
                    ShortcutTile(m.icon, stringResource(m.title), Modifier.fillMaxWidth(), stringResource(m.subtitle)) { nav.open(m.route) }
                }
            }
        }
    }
}
