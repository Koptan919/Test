package com.noor.app

import android.content.Context
import android.graphics.Color
import android.os.Bundle
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
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noor.app.data.prefs.ThemeMode
import com.noor.app.ui.navigation.NoorApp
import com.noor.app.ui.theme.NoorTheme
import com.noor.app.ui.theme.isDarkTheme
import com.noor.app.util.LocaleManager

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as NoorApplication).container
        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            val dark = isDarkTheme(settings?.themeMode ?: ThemeMode.DARK)
            LaunchedEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            NoorTheme(darkTheme = dark, textScale = settings?.textScale ?: 1f) {
                if (settings == null) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                } else {
                    NoorApp(onLanguageChanged = { recreate() })
                }
            }
        }
    }
}
