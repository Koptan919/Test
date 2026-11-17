package com.noor.app.ui

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Brightness4
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.noor.app.AppContainer
import com.noor.app.NoorApplication
import com.noor.app.R
import com.noor.core.prayer.Prayer

@Composable
fun isArabic(): Boolean = LocalConfiguration.current.locales[0].language == "ar"

@Composable
fun appContainer(): AppContainer = (LocalContext.current.applicationContext as NoorApplication).container

/** Creates a ViewModel with access to the app container. */
@Composable
inline fun <reified VM : ViewModel> noorViewModel(
    key: String? = null,
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = appContainer()
    return viewModel(key = key, factory = viewModelFactory { initializer { create(container) } })
}

fun prayerNameRes(prayer: Prayer): Int = when (prayer) {
    Prayer.FAJR -> R.string.prayer_fajr
    Prayer.SUNRISE -> R.string.prayer_sunrise
    Prayer.DHUHR -> R.string.prayer_dhuhr
    Prayer.ASR -> R.string.prayer_asr
    Prayer.SUNSET -> R.string.prayer_sunset
    Prayer.MAGHRIB -> R.string.prayer_maghrib
    Prayer.ISHA -> R.string.prayer_isha
    Prayer.MIDNIGHT -> R.string.prayer_midnight
}

fun prayerName(context: Context, prayer: Prayer): String = context.getString(prayerNameRes(prayer))

fun prayerIcon(prayer: Prayer): ImageVector = when (prayer) {
    Prayer.FAJR, Prayer.ISHA, Prayer.MIDNIGHT -> Icons.Outlined.NightsStay
    Prayer.SUNRISE, Prayer.DHUHR, Prayer.ASR -> Icons.Outlined.WbSunny
    Prayer.SUNSET, Prayer.MAGHRIB -> Icons.Outlined.Brightness4
}
