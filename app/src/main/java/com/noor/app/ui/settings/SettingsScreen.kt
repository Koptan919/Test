package com.noor.app.ui.settings

import android.Manifest
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.noor.app.AppContainer
import com.noor.app.R
import com.noor.app.data.prefs.AppSettings
import com.noor.app.data.prefs.ThemeMode
import com.noor.app.notifications.PrayerNotifier
import com.noor.app.ui.components.GlassCard
import com.noor.app.ui.components.IconBadge
import com.noor.app.ui.components.NoorTopBar
import com.noor.app.ui.isArabic
import com.noor.app.ui.navigation.NoorNavigator
import com.noor.app.ui.navigation.Routes
import com.noor.app.ui.noorViewModel
import com.noor.app.ui.prayerNameRes
import com.noor.app.ui.theme.LocalNoorPalette
import com.noor.app.util.AppLanguage
import com.noor.app.util.Formatters
import com.noor.app.util.LocaleManager
import com.noor.core.prayer.CalculationMethod
import com.noor.core.prayer.Prayer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    val settings: StateFlow<AppSettings?> = container.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun locationName(s: AppSettings, arabic: Boolean): String? = container.prayer.location(s)?.name(arabic)

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    fun setTheme(mode: ThemeMode) = launch { container.settings.setThemeMode(mode) }
    fun setTextScale(scale: Float) = launch { container.settings.setTextScale(scale) }
    fun setQuranFont(size: Float) = launch { container.settings.setQuranFontSize(size) }
    fun setMethod(method: CalculationMethod) = launch { container.settings.setCalculationMethod(method) }
    fun setHijriAdjust(days: Int) = launch { container.settings.setHijriAdjust(days) }
    fun setNotifications(enabled: Boolean) = launch { container.settings.setNotificationsEnabled(enabled) }
    fun setPrayerNotification(prayer: Prayer, enabled: Boolean) = launch { container.settings.setPrayerNotification(prayer, enabled) }
    fun setOccasionThemes(v: Boolean) = launch { container.settings.setOccasionThemes(v) }
    fun setReduceMotion(v: Boolean) = launch { container.settings.setReduceMotion(v) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(nav: NoorNavigator, onLanguageChanged: () -> Unit) {
    val vm = noorViewModel { SettingsViewModel(it) }
    val settingsState by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val arabic = isArabic()
    val palette = LocalNoorPalette.current

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.setNotifications(granted)
        if (!granted) toast(context, context.getString(R.string.settings_notifications_denied))
    }

    Column(Modifier.fillMaxSize()) {
        NoorTopBar(stringResource(R.string.nav_settings))
        val s = settingsState ?: return@Column
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SettingsCard(Icons.Outlined.Language, stringResource(R.string.settings_language)) {
                val current = LocaleManager.get(context)
                val options = listOf(
                    AppLanguage.SYSTEM to R.string.settings_language_system,
                    AppLanguage.ARABIC to R.string.settings_language_arabic,
                    AppLanguage.ENGLISH to R.string.settings_language_english,
                )
                Segmented(options.map { it.second }, options.indexOfFirst { it.first == current }) { i ->
                    val lang = options[i].first
                    if (current != lang) {
                        LocaleManager.set(context, lang)
                        onLanguageChanged()
                    }
                }
            }

            SettingsCard(Icons.Outlined.Palette, stringResource(R.string.settings_appearance)) {
                Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                val themes = listOf(ThemeMode.SYSTEM to R.string.settings_theme_system, ThemeMode.DARK to R.string.settings_theme_dark, ThemeMode.LIGHT to R.string.settings_theme_light)
                Segmented(themes.map { it.second }, themes.indexOfFirst { it.first == s.themeMode }) { vm.setTheme(themes[it].first) }
                Spacer(Modifier.height(14.dp))
                LabeledSlider(stringResource(R.string.settings_text_size), s.textScale, 0.85f..1.4f, { Formatters.digits("${(it * 100).toInt()}%", arabic) }, vm::setTextScale)
                LabeledSlider(stringResource(R.string.settings_quran_font), s.quranFontSize, AppSettings.MIN_QURAN_FONT..AppSettings.MAX_QURAN_FONT, { Formatters.number(it.toInt(), arabic) }, vm::setQuranFont)
                SwitchRow(stringResource(R.string.settings_occasion_themes), stringResource(R.string.settings_occasion_themes_desc), s.occasionThemes, true, vm::setOccasionThemes)
                SwitchRow(stringResource(R.string.settings_reduce_motion), stringResource(R.string.settings_reduce_motion_desc), s.reduceMotion, true, vm::setReduceMotion)
            }

            SettingsCard(Icons.Outlined.Tune, stringResource(R.string.settings_prayer)) {
                SettingsLink(Icons.Outlined.LocationOn, stringResource(R.string.settings_location), vm.locationName(s, arabic) ?: stringResource(R.string.settings_location_none)) { nav.open(Routes.CITY) }
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Text(stringResource(R.string.settings_method), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                val methods = listOf(CalculationMethod.JAFARI to R.string.method_jafari, CalculationMethod.TEHRAN to R.string.method_tehran)
                Segmented(methods.map { it.second }, methods.indexOfFirst { it.first == s.calculationMethod }) { vm.setMethod(methods[it].first) }
                Text(
                    stringResource(if (s.calculationMethod == CalculationMethod.JAFARI) R.string.method_jafari_desc else R.string.method_tehran_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_hijri_adjust), style = MaterialTheme.typography.labelLarge)
                        Text(stringResource(R.string.settings_hijri_adjust_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalIconButton(onClick = { vm.setHijriAdjust(s.hijriAdjust - 1) }, enabled = s.hijriAdjust > -2) {
                        Icon(Icons.Outlined.Remove, stringResource(R.string.action_decrease))
                    }
                    Text(
                        Formatters.digits(if (s.hijriAdjust > 0) "+${s.hijriAdjust}" else "${s.hijriAdjust}", arabic),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 10.dp),
                    )
                    FilledTonalIconButton(onClick = { vm.setHijriAdjust(s.hijriAdjust + 1) }, enabled = s.hijriAdjust < 2) {
                        Icon(Icons.Outlined.Add, stringResource(R.string.action_increase))
                    }
                }
            }

            SettingsCard(Icons.Outlined.Notifications, stringResource(R.string.settings_notifications)) {
                SwitchRow(
                    title = stringResource(R.string.settings_notifications_enable),
                    subtitle = stringResource(if (s.hasLocation) R.string.settings_notifications_desc else R.string.settings_notifications_need_location),
                    checked = s.notificationsEnabled,
                    enabled = s.hasLocation,
                ) { enable ->
                    if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !PrayerNotifier.canNotify(context)) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        vm.setNotifications(enable)
                    }
                }
                if (s.notificationsEnabled) {
                    Prayer.prayers.forEach { p ->
                        SwitchRow(stringResource(prayerNameRes(p)), null, p in s.notifyPrayers, true) { vm.setPrayerNotification(p, it) }
                    }
                }
            }

            GlassCard(Modifier.fillMaxWidth(), onClick = { nav.open(Routes.SETTINGS_ADVANCED) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Outlined.Tune, size = 36.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_advanced), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.settings_advanced_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Segmented(labels: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    val palette = LocalNoorPalette.current
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        labels.forEachIndexed { i, label ->
            SegmentedButton(
                selected = selected == i,
                onClick = { onSelect(i) },
                shape = SegmentedButtonDefaults.itemShape(i, labels.size),
                colors = SegmentedButtonDefaults.colors(activeContainerColor = palette.accent.copy(alpha = 0.2f), activeContentColor = palette.accent),
            ) { Text(stringResource(label), maxLines = 1) }
        }
    }
}

@Composable
internal fun SettingsCard(icon: ImageVector, title: String, content: @Composable ColumnScope.() -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, size = 36.dp)
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
internal fun SettingsLink(icon: ImageVector, title: String, value: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
        Icon(icon, null, tint = LocalNoorPalette.current.accent)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun SwitchRow(title: String, subtitle: String?, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    val palette = LocalNoorPalette.current
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = palette.accent, checkedThumbColor = MaterialTheme.colorScheme.surface),
        )
    }
}

@Composable
internal fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: (Float) -> String,
    onChange: (Float) -> Unit,
) {
    var local by remember(value) { mutableFloatStateOf(value) }
    val palette = LocalNoorPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        Text(display(local), style = MaterialTheme.typography.labelLarge, color = palette.accent)
    }
    Slider(
        value = local,
        onValueChange = { local = it },
        onValueChangeFinished = { onChange(local) },
        valueRange = range,
        colors = SliderDefaults.colors(thumbColor = palette.accent, activeTrackColor = palette.accent),
    )
}

internal fun toast(context: Context, text: String) = Toast.makeText(context, text, Toast.LENGTH_LONG).show()
