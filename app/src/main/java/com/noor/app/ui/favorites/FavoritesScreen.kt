package com.noor.app.ui.favorites

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.noor.app.AppContainer
import com.noor.app.R
import com.noor.app.data.db.FavoriteEntity
import com.noor.app.data.db.FavoriteType
import com.noor.app.data.db.favoriteType
import com.noor.app.ui.components.GlassCard
import com.noor.app.ui.components.IconBadge
import com.noor.app.ui.components.NoorTopBar
import com.noor.app.ui.components.UnavailableState
import com.noor.app.ui.isArabic
import com.noor.app.ui.navigation.NoorNavigator
import com.noor.app.ui.navigation.Routes
import com.noor.app.ui.noorViewModel
import com.noor.app.ui.theme.LocalNoorPalette
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoritesViewModel(private val container: AppContainer) : ViewModel() {
    val favorites: StateFlow<List<FavoriteEntity>?> = container.database.favoriteDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun remove(key: String) {
        viewModelScope.launch { container.database.favoriteDao().delete(key) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoritesScreen(nav: NoorNavigator) {
    val vm = noorViewModel { FavoritesViewModel(it) }
    val all by vm.favorites.collectAsStateWithLifecycle()
    val arabic = isArabic()
    var filter by rememberSaveable { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        NoorTopBar(stringResource(R.string.nav_saved))
        val list = all ?: return@Column
        if (list.isEmpty()) {
            UnavailableState(
                icon = Icons.Outlined.FavoriteBorder,
                title = stringResource(R.string.favorites_empty_title),
                message = stringResource(R.string.favorites_empty_msg),
                modifier = Modifier.padding(18.dp),
                actionLabel = stringResource(R.string.module_quran),
                onAction = { nav.open(Routes.QURAN) },
            )
            return@Column
        }
        val filters = listOf(
            null to R.string.filter_all,
            FavoriteType.SURAH.name to R.string.filter_surahs,
            FavoriteType.AYAH.name to R.string.filter_ayahs,
            FavoriteType.DUA.name to R.string.filter_duas,
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(filters) { (key, label) ->
                FilterChip(
                    selected = filter == key,
                    onClick = { filter = key },
                    label = { Text(stringResource(label)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LocalNoorPalette.current.accent.copy(alpha = 0.2f),
                        selectedLabelColor = LocalNoorPalette.current.accent,
                    ),
                )
            }
        }
        val shown = list.filter { filter == null || it.type == filter }
        LazyColumn(
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(shown, key = { it.key }) { fav ->
                GlassCard(
                    Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .animateContentSize(),
                    onClick = { nav.open(routeFor(fav)) },
                    contentPadding = PaddingValues(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(
                            when (fav.favoriteType) {
                                FavoriteType.SURAH -> Icons.Outlined.AutoStories
                                FavoriteType.AYAH -> Icons.Outlined.BookmarkBorder
                                FavoriteType.DUA -> Icons.Outlined.Book
                            },
                            size = 40.dp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (arabic) fav.titleAr else fav.titleEn, style = MaterialTheme.typography.titleSmall)
                            fav.preview?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        IconButton(onClick = { vm.remove(fav.key) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.action_remove), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

private fun routeFor(fav: FavoriteEntity): String = when (fav.favoriteType) {
    FavoriteType.SURAH -> Routes.reader(fav.refId.toIntOrNull() ?: 1)
    FavoriteType.AYAH -> {
        val parts = fav.refId.split(":")
        Routes.reader(parts.getOrNull(0)?.toIntOrNull() ?: 1, parts.getOrNull(1)?.toIntOrNull() ?: 0)
    }
    FavoriteType.DUA -> Routes.dua(fav.refId)
}
