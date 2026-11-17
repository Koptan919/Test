package com.noor.app.ui.explore

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noor.app.R
import com.noor.app.ui.appContainer
import com.noor.app.ui.components.GlassCard
import com.noor.app.ui.components.IconBadge
import com.noor.app.ui.components.NoorTopBar
import com.noor.app.ui.components.SectionHeader
import com.noor.app.ui.isArabic
import com.noor.app.ui.navigation.NoorNavigator
import com.noor.app.ui.navigation.Routes
import com.noor.app.ui.theme.LocalNoorPalette
import com.noor.app.util.Formatters
import com.noor.core.content.DuaCategory

private data class Shelf(val icon: ImageVector, val title: Int, val subtitle: Int, val route: String, val category: DuaCategory? = null)

/** The religious library: every book and collection in one calm list. */
@Composable
fun LibraryScreen(nav: NoorNavigator) {
    val container = appContainer()
    val pack by container.duas.pack.collectAsStateWithLifecycle()
    val arabic = isArabic()
    val entries = container.duas.entries(pack)

    fun counts(c: DuaCategory): Pair<Int, Int> {
        val list = entries.filter { it.category == c }
        return list.count { container.duas.isAvailable(it, pack) } to list.size
    }

    val sections = listOf(
        R.string.library_section_quran to listOf(
            Shelf(Icons.Outlined.AutoStories, R.string.module_quran, R.string.library_quran_desc, Routes.QURAN),
            Shelf(Icons.Outlined.Spa, R.string.category_quranic, R.string.module_quranic_sub, Routes.duas(DuaCategory.QURANIC.key), DuaCategory.QURANIC),
        ),
        R.string.library_section_mafatih to listOf(
            Shelf(Icons.Outlined.Book, R.string.category_supplications, R.string.module_duas_sub, Routes.duas(DuaCategory.SUPPLICATIONS.key), DuaCategory.SUPPLICATIONS),
            Shelf(Icons.Outlined.Place, R.string.category_ziyarat, R.string.module_ziyarat_sub, Routes.duas(DuaCategory.ZIYARAT.key), DuaCategory.ZIYARAT),
            Shelf(Icons.Outlined.NightsStay, R.string.category_munajat, R.string.module_munajat_sub, Routes.duas(DuaCategory.MUNAJAT.key), DuaCategory.MUNAJAT),
            Shelf(Icons.Outlined.TouchApp, R.string.category_tasbihat, R.string.library_tasbihat_desc, Routes.duas(DuaCategory.TASBIHAT.key), DuaCategory.TASBIHAT),
        ),
        R.string.library_section_books to listOf(
            Shelf(Icons.AutoMirrored.Outlined.MenuBook, R.string.category_sahifa, R.string.library_sahifa_desc, Routes.duas(DuaCategory.SAHIFA.key), DuaCategory.SAHIFA),
            Shelf(Icons.AutoMirrored.Outlined.MenuBook, R.string.category_nahj, R.string.library_nahj_desc, Routes.duas(DuaCategory.NAHJ.key), DuaCategory.NAHJ),
        ),
        R.string.library_section_more to listOf(
            Shelf(Icons.Outlined.Headphones, R.string.module_audio, R.string.module_audio_sub, Routes.AUDIO),
            Shelf(Icons.Outlined.AccountBalance, R.string.marja_title, R.string.marja_subtitle, Routes.MARJA),
        ),
    )

    Column(Modifier.fillMaxSize()) {
        NoorTopBar(stringResource(R.string.module_library), onBack = nav::back)
        LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            sections.forEach { (header, shelves) ->
                item(key = "h$header") { SectionHeader(stringResource(header)) }
                items(shelves, key = { it.route }) { shelf ->
                    val count = shelf.category?.let { counts(it) }
                    GlassCard(Modifier.fillMaxWidth(), onClick = { nav.open(shelf.route) }, contentPadding = PaddingValues(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(shelf.icon, size = 42.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(shelf.title), style = MaterialTheme.typography.titleSmall)
                                Text(stringResource(shelf.subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                if (count != null) {
                                    Text(
                                        stringResource(R.string.library_available_count, Formatters.number(count.first, arabic), Formatters.number(count.second, arabic)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LocalNoorPalette.current.accent,
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
}

private data class Marja(val nameAr: String, val nameEn: String, val url: String)

/** Official websites only. Noor never presents rulings itself. */
private val marjaOffices = listOf(
    Marja("السيد علي الحسيني السيستاني", "Sayyid Ali al-Husayni al-Sistani", "https://www.sistani.org"),
    Marja("السيد علي الحسيني الخامنئي", "Sayyid Ali al-Husayni Khamenei", "https://www.leader.ir"),
    Marja("الشيخ ناصر مكارم الشيرازي", "Shaykh Naser Makarem Shirazi", "https://makarem.ir"),
    Marja("الشيخ حسين وحيد الخراساني", "Shaykh Hossein Wahid Khorasani", "https://wahidkhorasani.com"),
    Marja("الشيخ بشير حسين النجفي", "Shaykh Bashir Hussain al-Najafi", "https://www.alnajafy.com"),
)

@Composable
fun MarjaScreen(nav: NoorNavigator) {
    val context = LocalContext.current
    val arabic = isArabic()
    Column(Modifier.fillMaxSize()) {
        NoorTopBar(stringResource(R.string.marja_title), onBack = nav::back)
        LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.marja_notice), style = MaterialTheme.typography.bodyMedium)
                }
            }
            items(marjaOffices, key = { it.url }) { m ->
                val host = Uri.parse(m.url).host.orEmpty()
                GlassCard(
                    Modifier.fillMaxWidth(),
                    onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(m.url))) } },
                    onClickLabel = stringResource(R.string.marja_open),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Outlined.AccountBalance, size = 42.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (arabic) m.nameAr else m.nameEn, style = MaterialTheme.typography.titleSmall)
                            Text(host, style = MaterialTheme.typography.bodySmall, color = LocalNoorPalette.current.accent)
                        }
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
