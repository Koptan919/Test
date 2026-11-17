package com.noor.app.ui.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.noor.app.R
import com.noor.app.data.remote.ReleaseInfo
import com.noor.app.ui.appContainer
import com.noor.app.ui.audio.AudioScreen
import com.noor.app.ui.calendar.CalendarScreen
import com.noor.app.ui.components.NoorBackground
import com.noor.app.ui.duas.DuaListScreen
import com.noor.app.ui.duas.DuaReaderScreen
import com.noor.app.ui.explore.ExploreScreen
import com.noor.app.ui.explore.LibraryScreen
import com.noor.app.ui.explore.MarjaScreen
import com.noor.app.ui.favorites.FavoritesScreen
import com.noor.app.ui.home.HomeScreen
import com.noor.app.ui.prayer.CityPickerScreen
import com.noor.app.ui.prayer.PrayerTimesScreen
import com.noor.app.ui.qibla.QiblaScreen
import com.noor.app.ui.quran.QuranReaderScreen
import com.noor.app.ui.quran.SurahListScreen
import com.noor.app.ui.settings.AdvancedSettingsScreen
import com.noor.app.ui.settings.SettingsScreen
import com.noor.app.ui.tasbih.TasbihScreen
import com.noor.app.ui.theme.LocalNoorPalette
import com.noor.app.ui.theme.LocalReduceMotion
import com.noor.app.util.Formatters
import kotlinx.coroutines.launch

object Routes {
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val FAVORITES = "favorites"
    const val SETTINGS = "settings"
    const val SETTINGS_ADVANCED = "settings/advanced"
    const val QURAN = "quran"
    const val READER = "quran/{surah}?ayah={ayah}"
    const val DUAS = "duas?category={category}"
    const val DUA = "dua/{id}"
    const val PRAYER = "prayer"
    const val CITY = "city"
    const val TASBIH = "tasbih"
    const val CALENDAR = "calendar"
    const val QIBLA = "qibla"
    const val AUDIO = "audio"
    const val LIBRARY = "library"
    const val MARJA = "marja"

    fun reader(surah: Int, ayah: Int = 0) = "quran/$surah?ayah=$ayah"
    fun duas(category: String? = null) = if (category == null) "duas" else "duas?category=$category"
    fun dua(id: String) = "dua/$id"
}

/** Navigation actions shared by all screens. */
class NoorNavigator(private val nav: NavHostController) {
    fun open(route: String) = nav.navigate(route) { launchSingleTop = true }
    fun back() { nav.popBackStack() }

    /** Switches tabs while preserving each tab's back stack and scroll state. */
    fun switchTab(route: String) = nav.navigate(route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private data class Tab(val route: String, val label: Int, val icon: ImageVector, val selectedIcon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, R.string.nav_home, Icons.Outlined.Home, Icons.Filled.Home),
    Tab(Routes.EXPLORE, R.string.nav_explore, Icons.Outlined.Explore, Icons.Filled.Explore),
    Tab(Routes.FAVORITES, R.string.nav_saved, Icons.Outlined.BookmarkBorder, Icons.Filled.Bookmark),
    Tab(Routes.SETTINGS, R.string.nav_settings, Icons.Outlined.Settings, Icons.Filled.Settings),
)

private const val DURATION = 300

@Composable
fun NoorApp(onLanguageChanged: () -> Unit) {
    val nav = rememberNavController()
    val navigator = remember(nav) { NoorNavigator(nav) }
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBar = tabs.any { it.route == currentRoute }
    val palette = LocalNoorPalette.current
    val reduce = LocalReduceMotion.current
    val haptics = LocalHapticFeedback.current

    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (reduce) fadeIn(tween(120)) else fadeIn(tween(DURATION, easing = FastOutSlowInEasing)) +
            scaleIn(tween(DURATION, easing = FastOutSlowInEasing), initialScale = 0.97f) +
            slideInVertically(tween(DURATION, easing = FastOutSlowInEasing)) { it / 30 }
    }
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = { fadeOut(tween(if (reduce) 90 else DURATION / 2)) }
    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (reduce) fadeIn(tween(120)) else fadeIn(tween(DURATION)) + scaleIn(tween(DURATION), initialScale = 1.02f)
    }
    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (reduce) fadeOut(tween(90)) else fadeOut(tween(DURATION)) + scaleOut(tween(DURATION), targetScale = 0.97f)
    }

    NoorBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            bottomBar = {
                if (showBar) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (palette.isDark) 0.92f else 0.96f),
                        tonalElevation = 0.dp,
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        navigator.switchTab(tab.route)
                                    }
                                },
                                icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                                label = { Text(stringResource(tab.label), style = MaterialTheme.typography.labelMedium) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = palette.accent,
                                    selectedTextColor = palette.accent,
                                    indicatorColor = palette.accent.copy(alpha = 0.16f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            },
        ) { padding ->
            // On tablets and foldables, keep a comfortable reading width, centred.
            androidx.compose.foundation.layout.Box(
                Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding()),
                contentAlignment = androidx.compose.ui.Alignment.TopCenter,
            ) {
            NavHost(
                navController = nav,
                startDestination = Routes.HOME,
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize(),
                enterTransition = enter,
                exitTransition = exit,
                popEnterTransition = popEnter,
                popExitTransition = popExit,
            ) {
                composable(Routes.HOME) { HomeScreen(navigator) }
                composable(Routes.EXPLORE) { ExploreScreen(navigator) }
                composable(Routes.FAVORITES) { FavoritesScreen(navigator) }
                composable(Routes.SETTINGS) { SettingsScreen(navigator, onLanguageChanged) }
                composable(Routes.SETTINGS_ADVANCED) { AdvancedSettingsScreen(navigator) }
                composable(Routes.QURAN) { SurahListScreen(navigator) }
                composable(
                    Routes.READER,
                    arguments = listOf(
                        navArgument("surah") { type = NavType.IntType },
                        navArgument("ayah") { type = NavType.IntType; defaultValue = 0 },
                    ),
                ) { entry ->
                    val surah = entry.arguments?.getInt("surah") ?: 1
                    val ayah = entry.arguments?.getInt("ayah") ?: 0
                    QuranReaderScreen(navigator, surah.coerceIn(1, 114), ayah)
                }
                composable(
                    Routes.DUAS,
                    arguments = listOf(navArgument("category") { type = NavType.StringType; nullable = true; defaultValue = null }),
                ) { entry -> DuaListScreen(navigator, entry.arguments?.getString("category")) }
                composable(
                    Routes.DUA,
                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                ) { entry -> DuaReaderScreen(navigator, entry.arguments?.getString("id").orEmpty()) }
                composable(Routes.PRAYER) { PrayerTimesScreen(navigator) }
                composable(Routes.CITY) { CityPickerScreen(navigator) }
                composable(Routes.TASBIH) { TasbihScreen(navigator) }
                composable(Routes.CALENDAR) { CalendarScreen(navigator) }
                composable(Routes.QIBLA) { QiblaScreen(navigator) }
                composable(Routes.AUDIO) { AudioScreen(navigator) }
                composable(Routes.LIBRARY) { LibraryScreen(navigator) }
                composable(Routes.MARJA) { MarjaScreen(navigator) }
            }
            }
        }
        UpdatePrompt()
    }
}

/** Shown when a newer stable release exists, or a verified update is already downloaded. */
@Composable
private fun UpdatePrompt() {
    val container = appContainer()
    val release by container.updates.prompt.collectAsStateWithLifecycle()
    val installState by container.updater.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    // A verified update downloaded in the background is offered as soon as the app opens.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val s = container.settings.current()
        if (s.autoUpdateCheck && container.updater.pendingUpdate() != null && container.connectivity.isOnline()) {
            val check = container.updates.check()
            if (check is com.noor.app.data.remote.UpdateCheck.Available) container.updates.showPrompt(check.release)
        }
    }
    val r = release ?: return
    val update = rememberUpdateAction()
    UpdateDialog(
        release = r,
        busy = installState is com.noor.app.data.remote.InstallState.Downloading || installState is com.noor.app.data.remote.InstallState.Installing,
        onUpdate = { update(r) },
        onLater = { scope.launch { container.updates.dismiss(r) } },
    )
}

/**
 * Downloads, verifies and installs [ReleaseInfo] through the system installer. If Android
 * has not yet allowed Noor to install updates, opens that one-time permission screen.
 * Falls back to the official download page if the release cannot be verified.
 */
@Composable
fun rememberUpdateAction(): (ReleaseInfo) -> Unit {
    val container = appContainer()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(container) {
        { release ->
            scope.launch {
                if (!container.updater.canInstall()) {
                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.packageName))
                    runCatching { context.startActivity(intent) }
                    android.widget.Toast.makeText(context, context.getString(R.string.update_allow_install), android.widget.Toast.LENGTH_LONG).show()
                    return@launch
                }
                val ready = container.updater.state.value as? com.noor.app.data.remote.InstallState.Ready
                val file = if (ready != null && ready.release.tag == release.tag && ready.file.exists()) ready.file
                else runCatching { container.updater.downloadAndVerify(release) }.getOrNull()
                if (file == null) {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(release.pageUrl))) }
                    container.updates.closePrompt()
                    return@launch
                }
                runCatching { container.updater.install(file) }
                    .onSuccess { container.updates.closePrompt() }
                    .onFailure { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(release.pageUrl))) } }
            }
        }
    }
}

@Composable
fun UpdateDialog(release: ReleaseInfo, onUpdate: () -> Unit, onLater: () -> Unit, busy: Boolean = false) {
    val palette = LocalNoorPalette.current
    val arabic = com.noor.app.ui.isArabic()
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text(stringResource(R.string.update_title), style = MaterialTheme.typography.titleLarge) },
        text = {
            androidx.compose.foundation.layout.Column(
                Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    stringResource(R.string.update_version, Formatters.digits(release.tag.removePrefix("v"), arabic)),
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.accent,
                )
                Text(stringResource(R.string.update_data_safe), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (release.notes.isNotBlank()) {
                    Text(
                        stringResource(R.string.update_changelog),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    )
                    Text(release.notes.trim(), style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onUpdate,
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = MaterialTheme.colorScheme.background),
            ) {
                if (busy) {
                    androidx.compose.material3.CircularProgressIndicator(Modifier.padding(end = 8.dp).heightIn(max = 18.dp), strokeWidth = 2.dp)
                    Text(stringResource(R.string.update_installing))
                } else {
                    Text(stringResource(R.string.update_now))
                }
            }
        },
        dismissButton = { TextButton(onClick = onLater) { Text(stringResource(R.string.update_later)) } },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    )
}
