package com.noor.app.ui.navigation

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
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import com.noor.app.ui.calendar.CalendarScreen
import com.noor.app.ui.components.NoorBackground
import com.noor.app.ui.duas.DuaListScreen
import com.noor.app.ui.duas.DuaReaderScreen
import com.noor.app.ui.explore.ExploreScreen
import com.noor.app.ui.favorites.FavoritesScreen
import com.noor.app.ui.home.HomeScreen
import com.noor.app.ui.prayer.CityPickerScreen
import com.noor.app.ui.prayer.PrayerTimesScreen
import com.noor.app.ui.quran.QuranReaderScreen
import com.noor.app.ui.quran.SurahListScreen
import com.noor.app.ui.settings.SettingsScreen
import com.noor.app.ui.tasbih.TasbihScreen
import com.noor.app.ui.theme.LocalNoorPalette

object Routes {
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val FAVORITES = "favorites"
    const val SETTINGS = "settings"
    const val QURAN = "quran"
    const val READER = "quran/{surah}?ayah={ayah}"
    const val DUAS = "duas?category={category}"
    const val DUA = "dua/{id}"
    const val PRAYER = "prayer"
    const val CITY = "city"
    const val TASBIH = "tasbih"
    const val CALENDAR = "calendar"

    fun reader(surah: Int, ayah: Int = 0) = "quran/$surah?ayah=$ayah"
    fun duas(category: String? = null) = if (category == null) "duas" else "duas?category=$category"
    fun dua(id: String) = "dua/$id"
}

/** Navigation actions shared by all screens. */
class NoorNavigator(private val nav: NavHostController) {
    fun open(route: String) = nav.navigate(route) { launchSingleTop = true }
    fun back() { nav.popBackStack() }
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
    Tab(Routes.FAVORITES, R.string.nav_favorites, Icons.Outlined.FavoriteBorder, Icons.Filled.Favorite),
    Tab(Routes.SETTINGS, R.string.nav_settings, Icons.Outlined.Settings, Icons.Filled.Settings),
)

private const val DURATION = 320

private fun AnimatedContentTransitionScope<NavBackStackEntry>.enter(): EnterTransition =
    fadeIn(tween(DURATION, easing = FastOutSlowInEasing)) +
        scaleIn(tween(DURATION, easing = FastOutSlowInEasing), initialScale = 0.96f) +
        slideInVertically(tween(DURATION, easing = FastOutSlowInEasing)) { it / 24 }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.exit(): ExitTransition =
    fadeOut(tween(DURATION / 2))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnter(): EnterTransition =
    fadeIn(tween(DURATION)) + scaleIn(tween(DURATION), initialScale = 1.03f)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popExit(): ExitTransition =
    fadeOut(tween(DURATION)) + scaleOut(tween(DURATION), targetScale = 0.96f) +
        slideOutVertically(tween(DURATION)) { it / 24 }

@Composable
fun NoorApp(onLanguageChanged: () -> Unit) {
    val nav = rememberNavController()
    val navigator = NoorNavigator(nav)
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBar = tabs.any { it.route == currentRoute }
    val palette = LocalNoorPalette.current

    NoorBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (showBar) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (palette.isDark) 0.86f else 0.94f),
                        tonalElevation = 0.dp,
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigator.switchTab(tab.route) },
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
            NavHost(
                navController = nav,
                startDestination = Routes.HOME,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding()),
                enterTransition = { enter() },
                exitTransition = { exit() },
                popEnterTransition = { popEnter() },
                popExitTransition = { popExit() },
            ) {
                composable(Routes.HOME) { HomeScreen(navigator) }
                composable(Routes.EXPLORE) { ExploreScreen(navigator) }
                composable(Routes.FAVORITES) { FavoritesScreen(navigator) }
                composable(Routes.SETTINGS) { SettingsScreen(navigator, onLanguageChanged) }
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
            }
        }
    }
}
