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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.noor.app.AppContainer
import com.noor.app.BuildConfig
import com.noor.app.R
import com.noor.app.data.duas.ContentPack
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
    val pack: StateFlow<ContentPack?> = container.duas.pack

    init {
        viewModelScope.launch { container.duas.ensurePackLoaded() }
    }

    fun locationName(s: AppSettings, arabic: Boolean): String? = container.prayer.location(s)?.name(arabic)

    fun setTheme(mode: ThemeMode) = launch { container.settings.setThemeMode(mode) }
    fun setTextScale(scale: Float) = launch { container.settings.setTextScale(scale) }
    fun setQuranFont(size: Float) = launch { container.settings.setQuranFontSize(size) }
    fun setMethod(method: CalculationMethod) = launch { container.settings.setCalculationMethod(method) }
    fun setHijriAdjust(days: Int) = launch { container.settings.setHijriAdjust(days) }
    fun setNotifications(enabled: Boolean) = launch { container.settings.setNotificationsEnabled(enabled) }
    fun setPrayerNotification(prayer: Prayer, enabled: Boolean) = launch { container.settings.setPrayerNotification(prayer, enabled) }
    fun removePack() = launch { container.duas.removePack() }

    fun importPack(uri: android.net.Uri, onResult: (Result<Int>) -> Unit) = launch {
        onResult(runCatching { container.duas.importPack(uri) })
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(nav: NoorNavigator, onLanguageChanged: () -> Unit) {
    val vm = noorViewModel { SettingsViewModel(it) }
    val settingsState by vm.settings.collectAsStateWithLifecycle()
    val pack by vm.pack.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val arabic = isArabic()
    val palette = LocalNoorPalette.current

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.setNotifications(granted)
        if (!granted) toast(context, context.getString(R.string.settings_notifications_denied))
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            vm.importPack(uri) { result ->
                result.onSuccess { toast(context, context.getString(R.string.settings_pack_imported, Formatters.number(it, LocaleManager.isArabic(context)))) }
                    .onFailure { toast(context, context.getString(R.string.settings_pack_failed, it.message ?: "")) }
            }
        }
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
            // Language
            SettingsCard(Icons.Outlined.Language, stringResource(R.string.settings_language)) {
                val current = LocaleManager.get(context)
                val options = listOf(
                    AppLanguage.SYSTEM to R.string.settings_language_system,
                    AppLanguage.ARABIC to R.string.settings_language_arabic,
                    AppLanguage.ENGLISH to R.string.settings_language_english,
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    options.forEachIndexed { i, (lang, label) ->
                        SegmentedButton(
                            selected = current == lang,
                            onClick = {
                                if (current != lang) {
                                    LocaleManager.set(context, lang)
                                    onLanguageChanged()
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(i, options.size),
                            colors = SegmentedButtonDefaults.colors(activeContainerColor = palette.accent.copy(alpha = 0.2f), activeContentColor = palette.accent),
                        ) { Text(stringResource(label), maxLines = 1) }
                    }
                }
            }

            // Appearance
            SettingsCard(Icons.Outlined.Palette, stringResource(R.string.settings_appearance)) {
                Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                val themes = listOf(ThemeMode.SYSTEM to R.string.settings_theme_system, ThemeMode.DARK to R.string.settings_theme_dark, ThemeMode.LIGHT to R.string.settings_theme_light)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    themes.forEachIndexed { i, (mode, label) ->
                        SegmentedButton(
                            selected = s.themeMode == mode,
                            onClick = { vm.setTheme(mode) },
                            shape = SegmentedButtonDefaults.itemShape(i, themes.size),
                            colors = SegmentedButtonDefaults.colors(activeContainerColor = palette.accent.copy(alpha = 0.2f), activeContentColor = palette.accent),
                        ) { Text(stringResource(label), maxLines = 1) }
                    }
                }
                Spacer(Modifier.height(14.dp))
                LabeledSlider(
                    label = stringResource(R.string.settings_text_size),
                    value = s.textScale,
                    range = 0.85f..1.3f,
                    display = { Formatters.digits("${(it * 100).toInt()}%", arabic) },
                    onChange = vm::setTextScale,
                )
                LabeledSlider(
                    label = stringResource(R.string.settings_quran_font),
                    value = s.quranFontSize,
                    range = AppSettings.MIN_QURAN_FONT..AppSettings.MAX_QURAN_FONT,
                    display = { Formatters.number(it.toInt(), arabic) },
                    onChange = vm::setQuranFont,
                )
            }

            // Prayer times
            SettingsCard(Icons.Outlined.Tune, stringResource(R.string.settings_prayer)) {
                SettingsLink(
                    icon = Icons.Outlined.LocationOn,
                    title = stringResource(R.string.settings_location),
                    value = vm.locationName(s, arabic) ?: stringResource(R.string.settings_location_none),
                    onClick = { nav.open(Routes.CITY) },
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Text(stringResource(R.string.settings_method), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                val methods = listOf(CalculationMethod.JAFARI to R.string.method_jafari, CalculationMethod.TEHRAN to R.string.method_tehran)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    methods.forEachIndexed { i, (m, label) ->
                        SegmentedButton(
                            selected = s.calculationMethod == m,
                            onClick = { vm.setMethod(m) },
                            shape = SegmentedButtonDefaults.itemShape(i, methods.size),
                            colors = SegmentedButtonDefaults.colors(activeContainerColor = palette.accent.copy(alpha = 0.2f), activeContentColor = palette.accent),
                        ) { Text(stringResource(label), maxLines = 1) }
                    }
                }
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

            // Notifications
            SettingsCard(Icons.Outlined.Notifications, stringResource(R.string.settings_notifications)) {
                SwitchRow(
                    title = stringResource(R.string.settings_notifications_enable),
                    subtitle = stringResource(if (s.hasLocation) R.string.settings_notifications_desc else R.string.settings_notifications_need_location),
                    checked = s.notificationsEnabled,
                    enabled = s.hasLocation,
                    onChange = { enable ->
                        if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !PrayerNotifier.canNotify(context)) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            vm.setNotifications(enable)
                        }
                    },
                )
                if (s.notificationsEnabled) {
                    Prayer.prayers.forEach { p ->
                        SwitchRow(
                            title = stringResource(prayerNameRes(p)),
                            subtitle = null,
                            checked = p in s.notifyPrayers,
                            enabled = true,
                            onChange = { vm.setPrayerNotification(p, it) },
                        )
                    }
                }
            }

            // Content
            SettingsCard(Icons.Outlined.FileUpload, stringResource(R.string.settings_content)) {
                val p = pack
                Text(
                    if (p == null) stringResource(R.string.settings_pack_none)
                    else stringResource(R.string.settings_pack_active, p.name, Formatters.number(p.items.size, arabic), p.source),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(stringResource(R.string.settings_pack_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }) {
                        Icon(Icons.Outlined.FileUpload, null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.duas_import_pack))
                    }
                    if (p != null) {
                        TextButton(onClick = vm::removePack) {
                            Icon(Icons.Outlined.Delete, null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.action_remove))
                        }
                    }
                }
            }

            // About
            SettingsCard(Icons.Outlined.Info, stringResource(R.string.settings_about)) {
                Text(
                    "${if (arabic) BuildConfig.APP_NAME_AR else BuildConfig.APP_NAME_EN} · ${Formatters.digits(BuildConfig.VERSION_NAME, arabic)}",
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.about_privacy), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.about_sources_title), style = MaterialTheme.typography.labelLarge, color = palette.accent)
                listOf(R.string.about_source_quran, R.string.about_source_prayer, R.string.about_source_calendar, R.string.about_source_fonts).forEach {
                    Text("• " + stringResource(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SettingsCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, content: @Composable ColumnScope.() -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, size = 36.dp)
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun SettingsLink(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
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
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    val palette = LocalNoorPalette.current
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
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
private fun LabeledSlider(
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

private fun toast(context: Context, text: String) = Toast.makeText(context, text, Toast.LENGTH_LONG).show()
