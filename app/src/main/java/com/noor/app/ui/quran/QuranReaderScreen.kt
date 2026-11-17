package com.noor.app.ui.quran

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noor.app.R
import com.noor.app.data.db.FavoriteEntity
import com.noor.app.data.db.FavoriteType
import com.noor.app.data.prefs.AppSettings
import com.noor.app.ui.components.AyahNumber
import com.noor.app.ui.components.GlassCard
import com.noor.app.ui.components.HeroCard
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
import com.noor.core.quran.QuranMeta
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

private const val HEADER_ITEMS = 1

private enum class Sheet { NONE, READING, SOURCE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranReaderScreen(nav: NoorNavigator, surah: Int, initialAyah: Int) {
    val vm = noorViewModel(key = "reader_$surah") { QuranReaderViewModel(it, surah) }
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val favorites by vm.favoriteKeys.collectAsStateWithLifecycle()
    val arabic = isArabic()
    val palette = LocalNoorPalette.current
    val meta = QuranMeta.surah(surah)
    val context = LocalContext.current
    var sheet by rememberSaveable { mutableStateOf(Sheet.NONE) }
    var focus by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val surahFavKey = FavoriteEntity.keyOf(FavoriteType.SURAH, surah.toString())

    LaunchedEffect(surah) { vm.markOpened() }
    KeepScreenOn(settings?.keepScreenOn == true)
    ImmersiveMode(focus)

    Column(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = !focus, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            NoorTopBar(
                title = if (arabic) "سورة ${meta.nameArabic}" else meta.nameTransliterated,
                onBack = nav::back,
                actions = {
                    IconButton(onClick = { vm.toggleSurahFavorite() }) {
                        val fav = surahFavKey in favorites
                        Icon(
                            if (fav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = stringResource(if (fav) R.string.action_unfavorite else R.string.action_favorite),
                            tint = if (fav) palette.accent else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = { sheet = Sheet.READING }) {
                        Icon(Icons.Outlined.TextFields, contentDescription = stringResource(R.string.reading_settings))
                    }
                    IconButton(onClick = { focus = true }) {
                        Icon(Icons.Outlined.Fullscreen, contentDescription = stringResource(R.string.reading_focus))
                    }
                },
            )
        }
        if (focus) Spacer(Modifier.statusBarsPadding())

        when (val st = state) {
            ReaderState.Loading -> LoadingList()
            is ReaderState.Unavailable -> UnavailableState(
                icon = Icons.Outlined.ErrorOutline,
                title = stringResource(R.string.quran_unavailable_title),
                message = stringResource(R.string.quran_unavailable_msg),
                modifier = Modifier.padding(18.dp),
            )
            is ReaderState.Ready -> {
                val ayahs = st.ayahs
                val progress by remember {
                    derivedStateOf {
                        val idx = (listState.firstVisibleItemIndex - HEADER_ITEMS + 1).coerceIn(0, ayahs.size)
                        if (ayahs.isEmpty()) 0f else idx.toFloat() / ayahs.size
                    }
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = palette.accent,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                )
                LaunchedEffect(surah, ayahs.size) {
                    if (initialAyah in 1..ayahs.size) listState.scrollToItem(HEADER_ITEMS + initialAyah - 1)
                }
                LaunchedEffect(listState, ayahs.size) {
                    snapshotFlow { listState.firstVisibleItemIndex }.collectLatest { index ->
                        delay(700)
                        vm.saveProgress((index - HEADER_ITEMS + 1).coerceIn(1, ayahs.size))
                    }
                }

                val fontSize = settings?.quranFontSize ?: AppSettings.DEFAULT_QURAN_FONT
                val spacing = settings?.lineSpacing ?: AppSettings.DEFAULT_LINE_SPACING
                val tapInteraction = remember { MutableInteractionSource() }
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(if (focus) 4.dp else 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable(interactionSource = tapInteraction, indication = null, enabled = focus) { focus = false },
                ) {
                    item(key = "header") {
                        HeroCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "سورة ${meta.nameArabic}",
                                    style = MaterialTheme.typography.headlineLarge.copy(fontFamily = NoorFonts.Quran, fontSize = 34.sp, lineHeight = 60.sp),
                                    color = palette.onHero,
                                )
                                Text(
                                    stringResource(
                                        R.string.quran_surah_info,
                                        stringResource(if (meta.isMeccan) R.string.quran_meccan else R.string.quran_medinan),
                                        Formatters.number(meta.ayahCount, arabic),
                                    ) + (if (!arabic) " · ${meta.nameEnglish}" else ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.heroAccent,
                                )
                                if (QuranMeta.showsBismillahHeader(surah)) {
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        QuranMeta.BISMILLAH,
                                        style = MaterialTheme.typography.headlineSmall.copy(fontFamily = NoorFonts.Quran, fontSize = 26.sp, lineHeight = 52.sp),
                                        color = palette.onHero,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                    itemsIndexed(ayahs, key = { _, a -> a.number }) { _, ayah ->
                        val key = FavoriteEntity.keyOf(FavoriteType.AYAH, "${ayah.surah}:${ayah.number}")
                        val bookmarked = key in favorites
                        val highlight by animateColorAsState(
                            if (ayah.number == initialAyah) palette.accent.copy(alpha = 0.08f) else Color.Transparent,
                            label = "highlight",
                        )
                        val text = @Composable {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text(
                                    ayah.text + " ﴿" + Formatters.number(ayah.number, true) + "﴾",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = NoorFonts.Quran,
                                        fontSize = fontSize.sp,
                                        lineHeight = (fontSize * spacing).sp,
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Start,
                                )
                            }
                        }
                        if (focus) {
                            Column(Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) { text() }
                        } else {
                            GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                                Column(Modifier.background(highlight).padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AyahNumber(Formatters.number(ayah.number, arabic))
                                        Spacer(Modifier.weight(1f))
                                        IconButton(onClick = { copyToClipboard(context, ayah.text, "${meta.nameArabic} ${ayah.surah}:${ayah.number}") }) {
                                            Icon(Icons.Outlined.ContentCopy, stringResource(R.string.action_copy), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        IconButton(onClick = { vm.toggleAyahFavorite(ayah) }) {
                                            Icon(
                                                if (bookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                                stringResource(if (bookmarked) R.string.action_remove_bookmark else R.string.action_bookmark),
                                                tint = if (bookmarked) palette.accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    text()
                                }
                            }
                        }
                    }
                    item(key = "footer") {
                        Column(Modifier.navigationBarsPadding()) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                if (surah > 1) {
                                    TextButton(onClick = { nav.back(); nav.open(Routes.reader(surah - 1)) }) {
                                        Icon(Icons.AutoMirrored.Filled.NavigateBefore, null)
                                        Text(stringResource(R.string.quran_previous_surah))
                                    }
                                } else Spacer(Modifier.width(1.dp))
                                if (surah < 114) {
                                    TextButton(onClick = { nav.back(); nav.open(Routes.reader(surah + 1)) }) {
                                        Text(stringResource(R.string.quran_next_surah))
                                        Icon(Icons.AutoMirrored.Filled.NavigateNext, null)
                                    }
                                }
                            }
                            TextButton(onClick = { sheet = Sheet.SOURCE }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
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
    if (sheet != Sheet.NONE && s != null) {
        ModalBottomSheet(onDismissRequest = { sheet = Sheet.NONE }) {
            if (sheet == Sheet.READING) {
                ReadingSheet(s, arabic, vm::setFontSize, vm::setLineSpacing, vm::setKeepScreenOn)
            } else {
                SourceSheet(
                    stringResource(R.string.reading_source_details),
                    listOf(
                        stringResource(R.string.about_source_quran),
                        stringResource(R.string.quran_source_checksum, QuranMeta.TEXT_SHA256.take(16)),
                        stringResource(R.string.quran_source_cross_check),
                    ),
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String, label: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, "$text\n— $label"))
    Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show()
}
