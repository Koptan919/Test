package com.noor.app.ui.reading

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.noor.app.R
import com.noor.app.data.prefs.AppSettings
import com.noor.app.ui.theme.LocalNoorPalette
import com.noor.app.ui.theme.NoorFonts
import com.noor.app.util.Formatters
import com.noor.core.quran.QuranMeta

/** Keeps the screen on while [enabled] and the composable is shown. */
@Composable
fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}

/** Hides the system bars in distraction-free mode; restores them on exit. */
@Composable
fun ImmersiveMode(enabled: Boolean) {
    val context = LocalContext.current
    val view = LocalView.current
    DisposableEffect(enabled) {
        val window = (context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        if (enabled) {
            controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }
}

/** Reading preferences shared by the Quran and Dua readers. */
@Composable
fun ReadingSheet(
    settings: AppSettings,
    arabic: Boolean,
    onFontSize: (Float) -> Unit,
    onLineSpacing: (Float) -> Unit,
    onKeepScreenOn: (Boolean) -> Unit,
) {
    val palette = LocalNoorPalette.current
    var size by remember(settings.quranFontSize) { mutableFloatStateOf(settings.quranFontSize) }
    var spacing by remember(settings.lineSpacing) { mutableFloatStateOf(settings.lineSpacing) }
    Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 36.dp)) {
        Text(stringResource(R.string.reading_settings), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    QuranMeta.BISMILLAH,
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = NoorFonts.Quran, fontSize = size.sp, lineHeight = (size * spacing).sp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        LabeledValue(stringResource(R.string.settings_quran_font), Formatters.number(size.toInt(), arabic))
        Slider(
            value = size,
            onValueChange = { size = it },
            onValueChangeFinished = { onFontSize(size) },
            valueRange = AppSettings.MIN_QURAN_FONT..AppSettings.MAX_QURAN_FONT,
            colors = SliderDefaults.colors(thumbColor = palette.accent, activeTrackColor = palette.accent),
        )
        LabeledValue(stringResource(R.string.reading_line_spacing), Formatters.digits("%.1f×".format(java.util.Locale.ENGLISH, spacing), arabic))
        Slider(
            value = spacing,
            onValueChange = { spacing = it },
            onValueChangeFinished = { onLineSpacing(spacing) },
            valueRange = AppSettings.MIN_LINE_SPACING..AppSettings.MAX_LINE_SPACING,
            colors = SliderDefaults.colors(thumbColor = palette.accent, activeTrackColor = palette.accent),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.reading_keep_awake), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.reading_keep_awake_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = settings.keepScreenOn,
                onCheckedChange = onKeepScreenOn,
                colors = SwitchDefaults.colors(checkedTrackColor = palette.accent, checkedThumbColor = MaterialTheme.colorScheme.surface),
            )
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelLarge, color = LocalNoorPalette.current.accent)
    }
}

/** Source/attribution details shown from the readers. */
@Composable
fun SourceSheet(title: String, lines: List<String>) {
    Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 36.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(10.dp))
        lines.forEach {
            Text("• $it", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 6.dp))
        }
    }
}

