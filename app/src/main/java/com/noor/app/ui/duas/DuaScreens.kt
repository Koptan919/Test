package com.noor.app.ui.duas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.noor.app.AppContainer
import com.noor.app.R
import com.noor.app.data.db.FavoriteEntity
import com.noor.app.data.db.FavoriteType
import com.noor.app.data.duas.ContentPack
import com.noor.app.data.duas.DuaText
import com.noor.app.data.prefs.AppSettings
import com.noor.app.ui.components.AyahNumber
import com.noor.app.ui.components.GlassCard
import com.noor.app.ui.components.HeroCard
import com.noor.app.ui.components.IconBadge
import com.noor.app.ui.components.LoadingList
import com.noor.app.ui.components.NoorTopBar
import com.noor.app.ui.components.UnavailableState
import com.noor.app.ui.isArabic
import com.noor.app.ui.navigation.NoorNavigator
import com.noor.app.ui.navigation.Routes
import com.noor.app.ui.noorViewModel
import com.noor.app.ui.reading.ImmersiveMode
import com.noor.app.ui.reading.KeepScreenOn
import com.noor.app.ui.reading.ReadingSheet
import com.noor.app.ui.reading.SourceSheet
import com.noor.app.ui.theme.LocalNoorPalette
import com.noor.app.ui.theme.NoorFonts
import com.noor.app.util.Formatters
import com.noor.core.content.DuaCategory
import com.noor.core.content.DuaEntry
import com.noor.core.quran.QuranMeta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

fun categoryLabel(category: DuaCategory): Int = when (category) {
    DuaCategory.QURANIC -> R.string.category_quranic
    DuaCategory.SUPPLICATIONS -> R.string.category_supplications
    DuaCategory.ZIYARAT -> R.string.category_ziyarat
    DuaCategory.MUNAJAT -> R.string.category_munajat
    DuaCategory.TASBIHAT -> R.string.category_tasbihat
    DuaCategory.SAHIFA -> R.string.category_sahifa
    DuaCategory.NAHJ -> R.string.category_nahj
}

data class DuaListState(val entries: List<DuaEntry>, val available: Set<String>)

class DuaListViewModel(private val container: AppContainer) : ViewModel() {
    val state: StateFlow<DuaListState?> = container.duas.pack
        .map { pack: ContentPack? ->
            val entries = container.duas.entries(pack)
            DuaListState(entries, entries.filter { container.duas.isAvailable(it, pack) }.map { it.id }.toSet())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch { container.duas.ensurePackLoaded() }
    }
}

@Composable
fun DuaListScreen(nav: NoorNavigator, initialCategory: String?) {
    val vm = noorViewModel { DuaListViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val arabic = isArabic()
    val palette = LocalNoorPalette.current
    var category by rememberSaveable { mutableStateOf(initialCategory) }
    var query by rememberSaveable { mutableStateOf("") }
    val selected = DuaCategory.fromKey(category)

    Column(Modifier.fillMaxSize()) {
        NoorTopBar(stringResource(selected?.let { categoryLabel(it) } ?: R.string.module_duas), onBack = nav::back)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            placeholder = { Text(stringResource(R.string.duas_search_hint)) },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, stringResource(R.string.action_clear)) }
            },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = palette.accent, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant),
        )
        Spacer(Modifier.height(10.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = category == null,
                    onClick = { category = null },
                    label = { Text(stringResource(R.string.filter_all)) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = palette.accent.copy(alpha = 0.2f), selectedLabelColor = palette.accent),
                )
            }
            items(DuaCategory.entries) { c ->
                FilterChip(
                    selected = category == c.key,
                    onClick = { category = c.key },
                    label = { Text(stringResource(categoryLabel(c))) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = palette.accent.copy(alpha = 0.2f), selectedLabelColor = palette.accent),
                )
            }
        }
        val st = state
        if (st == null) {
            LoadingList()
            return@Column
        }
        val q = query.trim().lowercase()
        val list = st.entries.filter {
            (category == null || it.category.key == category) &&
                (q.isEmpty() || it.titleAr.contains(q) || it.titleEn.lowercase().contains(q))
        }
        LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            if (list.isEmpty() && q.isEmpty() && selected != null) {
                item {
                    UnavailableState(
                        icon = Icons.AutoMirrored.Outlined.MenuBook,
                        title = stringResource(R.string.library_book_empty_title, stringResource(categoryLabel(selected))),
                        message = stringResource(R.string.library_book_empty_msg),
                        actionLabel = stringResource(R.string.sync_now),
                        onAction = { nav.open(Routes.SETTINGS_ADVANCED) },
                    )
                }
            } else if (list.isEmpty()) {
                item { Text(stringResource(R.string.explore_no_results), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(list, key = { it.id }) { entry ->
                val available = entry.id in st.available
                GlassCard(Modifier.fillMaxWidth(), onClick = { nav.open(Routes.dua(entry.id)) }, contentPadding = PaddingValues(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Outlined.Book, size = 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(entry.title(arabic), style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(
                                stringResource(categoryLabel(entry.category)) + " · " + entry.source(arabic),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (available) Icons.Outlined.CheckCircle else Icons.Outlined.Lock,
                                    null,
                                    tint = if (available) palette.accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 4.dp).size(14.dp),
                                )
                                Text(
                                    stringResource(if (available) R.string.duas_verified_text else R.string.duas_text_not_bundled),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (available) palette.accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

sealed interface DuaReaderState {
    data object Loading : DuaReaderState
    data object NotFound : DuaReaderState
    data class Ready(val entry: DuaEntry, val text: DuaText) : DuaReaderState
}

class DuaReaderViewModel(private val container: AppContainer, private val id: String, private val arabic: Boolean) : ViewModel() {
    private val _state = MutableStateFlow<DuaReaderState>(DuaReaderState.Loading)
    val state: StateFlow<DuaReaderState> = _state.asStateFlow()
    val settings = container.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val isFavorite: StateFlow<Boolean> = container.database.favoriteDao().observeKeys()
        .map { FavoriteEntity.keyOf(FavoriteType.DUA, id) in it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        viewModelScope.launch {
            container.duas.ensurePackLoaded()
            container.duas.pack.collect {
                val entry = container.duas.byId(id)
                _state.value = if (entry == null) DuaReaderState.NotFound else DuaReaderState.Ready(entry, container.duas.text(entry, arabic))
            }
        }
        viewModelScope.launch { container.settings.addRecent("dua:$id") }
    }

    fun toggleFavorite() {
        val entry = container.duas.byId(id) ?: return
        viewModelScope.launch {
            val dao = container.database.favoriteDao()
            val key = FavoriteEntity.keyOf(FavoriteType.DUA, id)
            if (dao.count(key) > 0) dao.delete(key) else dao.upsert(FavoriteEntity(key, FavoriteType.DUA.name, id, entry.titleAr, entry.titleEn, entry.sourceEn))
        }
    }

    fun setFontSize(size: Float) { viewModelScope.launch { container.settings.setQuranFontSize(size) } }
    fun setLineSpacing(v: Float) { viewModelScope.launch { container.settings.setLineSpacing(v) } }
    fun setKeepScreenOn(v: Boolean) { viewModelScope.launch { container.settings.setKeepScreenOn(v) } }
}

private enum class DuaSheet { NONE, READING, SOURCE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuaReaderScreen(nav: NoorNavigator, id: String) {
    val arabic = isArabic()
    val vm = noorViewModel(key = "dua_$id") { DuaReaderViewModel(it, id, arabic) }
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val favorite by vm.isFavorite.collectAsStateWithLifecycle()
    val palette = LocalNoorPalette.current
    var sheet by rememberSaveable { mutableStateOf(DuaSheet.NONE) }
    var focus by rememberSaveable { mutableStateOf(false) }
    val fontSize = settings?.quranFontSize ?: AppSettings.DEFAULT_QURAN_FONT
    val spacing = settings?.lineSpacing ?: AppSettings.DEFAULT_LINE_SPACING
    val ready = state as? DuaReaderState.Ready

    KeepScreenOn(settings?.keepScreenOn == true && ready?.text !is DuaText.Unavailable)
    ImmersiveMode(focus)

    Column(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = !focus, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            NoorTopBar(ready?.entry?.title(arabic) ?: stringResource(R.string.module_duas), onBack = nav::back, actions = {
                if (ready != null) {
                    IconButton(onClick = vm::toggleFavorite) {
                        Icon(
                            if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            stringResource(if (favorite) R.string.action_unfavorite else R.string.action_favorite),
                            tint = if (favorite) palette.accent else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = { sheet = DuaSheet.READING }) { Icon(Icons.Outlined.TextFields, stringResource(R.string.reading_settings)) }
                    if (ready.text !is DuaText.Unavailable) {
                        IconButton(onClick = { focus = true }) { Icon(Icons.Outlined.Fullscreen, stringResource(R.string.reading_focus)) }
                    }
                }
            })
        }
        if (focus) Spacer(Modifier.statusBarsPadding())
        when (val st = state) {
            DuaReaderState.Loading -> LoadingList()
            DuaReaderState.NotFound -> UnavailableState(
                Icons.Outlined.CloudOff,
                stringResource(R.string.duas_not_found),
                stringResource(R.string.duas_not_found_msg),
                Modifier.padding(18.dp),
            )
            is DuaReaderState.Ready -> {
                val tap = remember { MutableInteractionSource() }
                LazyColumn(
                    contentPadding = PaddingValues(18.dp),
                    verticalArrangement = Arrangement.spacedBy(if (focus) 6.dp else 12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(interactionSource = tap, indication = null, enabled = focus) { focus = false },
                ) {
                    if (!focus) {
                        item {
                            HeroCard(Modifier.fillMaxWidth()) {
                                Text(st.entry.titleAr, style = MaterialTheme.typography.headlineSmall.copy(fontFamily = NoorFonts.Naskh, fontSize = 28.sp, lineHeight = 44.sp), color = palette.onHero)
                                if (!arabic) Text(st.entry.titleEn, style = MaterialTheme.typography.titleSmall, color = palette.heroAccent)
                                Spacer(Modifier.height(6.dp))
                                Text(stringResource(R.string.duas_source, st.entry.source(arabic)), style = MaterialTheme.typography.bodySmall, color = palette.onHero.copy(alpha = 0.8f))
                            }
                        }
                    }
                    when (val text = st.text) {
                        is DuaText.Quranic -> items(text.ayahs, key = { "${it.surah}:${it.number}" }) { ayah ->
                            val meta = QuranMeta.surah(ayah.surah)
                            GlassCard(Modifier.fillMaxWidth()) {
                                ArabicText(ayah.text, fontSize, spacing, quran = true)
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AyahNumber(Formatters.number(ayah.number, arabic), size = 28.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (arabic) "سورة ${meta.nameArabic}" else "${meta.nameTransliterated} ${ayah.surah}:${ayah.number}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        is DuaText.Lines -> {
                            items(text.lines.size) { i ->
                                if (focus) ArabicText(text.lines[i], fontSize, spacing, quran = false)
                                else GlassCard(Modifier.fillMaxWidth()) { ArabicText(text.lines[i], fontSize, spacing, quran = false) }
                            }
                            item { Text(stringResource(R.string.duas_text_source, text.source), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        DuaText.Unavailable -> item {
                            UnavailableState(
                                icon = Icons.Outlined.Lock,
                                title = stringResource(R.string.duas_unavailable_title),
                                message = stringResource(R.string.duas_unavailable_msg),
                                actionLabel = stringResource(R.string.sync_now),
                                onAction = { nav.open(Routes.SETTINGS_ADVANCED) },
                            )
                        }
                    }
                    val note = if (arabic) st.entry.noteAr else st.entry.noteEn
                    if (note != null && !focus) {
                        item {
                            GlassCard(Modifier.fillMaxWidth()) {
                                Text(note, style = MaterialTheme.typography.bodyMedium)
                                if (st.entry.id == "tasbih_zahra") {
                                    Spacer(Modifier.height(10.dp))
                                    FilledTonalButton(onClick = { nav.open(Routes.TASBIH) }) {
                                        Icon(Icons.Outlined.TouchApp, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.module_tasbih))
                                    }
                                }
                            }
                        }
                    }
                    if (!focus) {
                        item {
                            TextButton(onClick = { sheet = DuaSheet.SOURCE }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Outlined.Info, null)
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.reading_source_details))
                            }
                        }
                    }
                }
            }
        }
    }
    val s = settings
    if (sheet != DuaSheet.NONE && s != null && ready != null) {
        ModalBottomSheet(onDismissRequest = { sheet = DuaSheet.NONE }) {
            if (sheet == DuaSheet.READING) {
                ReadingSheet(s, arabic, vm::setFontSize, vm::setLineSpacing, vm::setKeepScreenOn)
            } else {
                val lines = buildList {
                    add(stringResource(R.string.duas_source, ready.entry.source(arabic)))
                    when (val t = ready.text) {
                        is DuaText.Quranic -> add(stringResource(R.string.about_source_quran))
                        is DuaText.Lines -> add(stringResource(R.string.duas_text_source, t.source))
                        DuaText.Unavailable -> add(stringResource(R.string.duas_text_not_bundled))
                    }
                }
                SourceSheet(stringResource(R.string.reading_source_details), lines)
            }
        }
    }
}

@Composable
private fun ArabicText(text: String, fontSize: Float, spacing: Float, quran: Boolean) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = if (quran) NoorFonts.Quran else NoorFonts.Naskh,
                fontSize = fontSize.sp,
                lineHeight = (fontSize * (if (quran) spacing else spacing * 0.92f)).sp,
            ),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
        )
    }
}

