package com.noor.app

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noor.app.data.prefs.ThemeMode
import com.noor.app.ui.navigation.NoorApp
import com.noor.app.ui.theme.NoorTheme
import com.noor.app.ui.theme.isDarkTheme
import com.noor.app.util.LocaleManager
import com.noor.core.calendar.Season
import com.noor.core.calendar.Seasons
import java.time.LocalDate

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as NoorApplication).container
        val systemReduceMotion = runCatching {
            Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)

        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            val s = settings
            val dark = isDarkTheme(s?.themeMode ?: ThemeMode.DARK)
            val today = LocalDate.now()
            val season = remember(s?.occasionThemes, s?.hijriAdjust, today) {
                if (s == null || !s.occasionThemes) Season.NONE else Seasons.of(container.hijri(s.hijriAdjust).toHijri(today))
            }
            LaunchedEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            LaunchedEffect(Unit) { container.updates.checkInBackgroundIfDue() }
            NoorTheme(
                darkTheme = dark,
                textScale = s?.textScale ?: 1f,
                season = season,
                reduceMotion = systemReduceMotion || s?.reduceMotion == true,
            ) {
                if (s == null) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                } else {
                    NoorApp(onLanguageChanged = { recreate() })
                }
            }
        }
    }
}
