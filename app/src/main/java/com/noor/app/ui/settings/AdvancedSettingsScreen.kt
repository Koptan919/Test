package com.noor.app.ui.settings

import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noor.app.BuildConfig
import com.noor.app.R
import com.noor.app.data.remote.SyncResult
import com.noor.app.data.remote.UpdateCheck
import com.noor.app.ui.appContainer
import com.noor.app.ui.components.NoorDialog
import com.noor.app.ui.components.NoorTopBar
import com.noor.app.ui.isArabic
import com.noor.app.ui.navigation.NoorNavigator
import com.noor.app.ui.navigation.UpdateDialog
import com.noor.app.ui.theme.LocalNoorPalette
import com.noor.app.util.Formatters
import com.noor.app.util.LocaleManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AdvancedSettingsScreen(nav: NoorNavigator) {
    val container = appContainer()
    val context = LocalContext.current
    val arabic = isArabic()
    val palette = LocalNoorPalette.current
    val scope = rememberCoroutineScope()
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    val pack by container.duas.pack.collectAsStateWithLifecycle()
    val sync by container.contentSync.state.collectAsStateWithLifecycle()
    val update by container.updates.state.collectAsStateWithLifecycle()
    val online by container.connectivity.online.collectAsStateWithLifecycle(initialValue = true)
    var cacheBytes by remember { mutableLongStateOf(-1L) }
    var confirmRemovePack by remember { mutableStateOf(false) }
    var showUpdate by remember { mutableStateOf(false) }

    fun refreshCache() = scope.launch { cacheBytes = withContext(Dispatchers.IO) { container.cacheSizeBytes() } }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        refreshCache()
        container.duas.ensurePackLoaded()
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching { container.duas.importPack(uri) }
                .onSuccess { toast(context, context.getString(R.string.settings_pack_imported, Formatters.number(it, LocaleManager.isArabic(context)))) }
                .onFailure { toast(context, context.getString(R.string.settings_pack_failed, it.message ?: "")) }
        }
    }

    Column(Modifier.fillMaxSize()) {
        NoorTopBar(stringResource(R.string.settings_advanced), onBack = nav::back)
        val s = settings ?: return@Column
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Content synchronisation.
            SettingsCard(Icons.Outlined.CloudSync, stringResource(R.string.sync_title)) {
                Text(stringResource(R.string.sync_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SwitchRow(stringResource(R.string.sync_auto), stringResource(R.string.sync_auto_desc), s.autoContentSync, true) {
                    scope.launch { container.settings.setAutoContentSync(it) }
                }
                val p = pack
                Text(
                    if (p == null) stringResource(R.string.settings_pack_none)
                    else stringResource(R.string.settings_pack_active, p.name, Formatters.number(p.items.size, arabic), p.source),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (s.lastContentSync > 0) {
                    Text(
                        stringResource(R.string.sync_last, DateUtils.getRelativeTimeSpanString(s.lastContentSync).toString()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(syncMessage(sync), style = MaterialTheme.typography.bodySmall, color = if (sync is SyncResult.Failed) MaterialTheme.colorScheme.error else palette.accent)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { scope.launch { container.contentSync.sync() } }, enabled = sync !is SyncResult.Running && online) {
                        if (sync is SyncResult.Running) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Outlined.CloudSync, null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.sync_now))
                    }
                }
                if (!online) Text(stringResource(R.string.offline_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Text(stringResource(R.string.settings_pack_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }) {
                        Icon(Icons.Outlined.FileUpload, null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.duas_import_pack))
                    }
                    if (p != null) {
                        TextButton(onClick = { confirmRemovePack = true }) {
                            Icon(Icons.Outlined.Delete, null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.action_remove))
                        }
                    }
                }
            }

            // Application updates.
            SettingsCard(Icons.Outlined.SystemUpdate, stringResource(R.string.updates_title)) {
                Text(stringResource(R.string.updates_installed, Formatters.digits(BuildConfig.VERSION_NAME, arabic)), style = MaterialTheme.typography.bodyMedium)
                SwitchRow(stringResource(R.string.updates_auto), stringResource(R.string.updates_auto_desc), s.autoUpdateCheck, true) {
                    scope.launch { container.settings.setAutoUpdateCheck(it) }
                }
                Text(updateMessage(update), style = MaterialTheme.typography.bodySmall, color = if (update is UpdateCheck.Failed) MaterialTheme.colorScheme.error else palette.accent)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                if (container.updates.check() is UpdateCheck.Available) showUpdate = true
                            }
                        },
                        enabled = update !is UpdateCheck.Checking,
                    ) {
                        if (update is UpdateCheck.Checking) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Outlined.SystemUpdate, null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.updates_check_now))
                    }
                    if (update is UpdateCheck.Available) {
                        TextButton(onClick = { showUpdate = true }) { Text(stringResource(R.string.update_now)) }
                    }
                }
                Text(stringResource(R.string.updates_source, BuildConfig.RELEASES_REPO), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Cache.
            SettingsCard(Icons.Outlined.Storage, stringResource(R.string.cache_title)) {
                Text(
                    if (cacheBytes < 0) "…" else stringResource(R.string.cache_size, Formatters.digits(Formatter.formatShortFileSize(context, cacheBytes), arabic)),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(stringResource(R.string.cache_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) { container.clearCache() }
                            refreshCache()
                            toast(context, context.getString(R.string.cache_cleared))
                        }
                    }) { Text(stringResource(R.string.cache_clear)) }
                    TextButton(onClick = { scope.launch { container.settings.clearRecents() } }) { Text(stringResource(R.string.cache_clear_recents)) }
                }
            }

            // Sources.
            SettingsCard(Icons.Outlined.Info, stringResource(R.string.settings_about)) {
                Text(
                    "${if (arabic) BuildConfig.APP_NAME_AR else BuildConfig.APP_NAME_EN} · ${Formatters.digits(BuildConfig.VERSION_NAME, arabic)}",
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.about_privacy), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.about_sources_title), style = MaterialTheme.typography.labelLarge, color = palette.accent)
                listOf(
                    R.string.about_source_quran, R.string.about_source_prayer, R.string.about_source_calendar,
                    R.string.about_source_qibla, R.string.about_source_texts, R.string.about_source_fonts,
                ).forEach {
                    Text("• " + stringResource(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (confirmRemovePack) {
        NoorDialog(
            title = stringResource(R.string.pack_remove_title),
            text = stringResource(R.string.pack_remove_msg),
            confirmLabel = stringResource(R.string.action_remove),
            onConfirm = {
                scope.launch {
                    container.duas.removePack()
                    container.settings.setSyncedContent(null, 0L)
                }
                confirmRemovePack = false
            },
            onDismiss = { confirmRemovePack = false },
        )
    }
    val available = update as? UpdateCheck.Available
    if (showUpdate && available != null) {
        UpdateDialog(available.release, onUpdate = { showUpdate = false }, onLater = { showUpdate = false })
    }
}

@Composable
private fun syncMessage(r: SyncResult): String = when (r) {
    SyncResult.Idle -> ""
    SyncResult.Running -> stringResource(R.string.sync_running)
    is SyncResult.Updated -> stringResource(R.string.sync_updated, r.version, r.texts.toString())
    SyncResult.UpToDate -> stringResource(R.string.sync_up_to_date)
    SyncResult.NothingPublished -> stringResource(R.string.sync_nothing)
    SyncResult.ImportedPackActive -> stringResource(R.string.sync_imported_active)
    is SyncResult.Failed -> if (r.offline) stringResource(R.string.offline_hint) else stringResource(R.string.sync_failed, r.reason)
}

@Composable
private fun updateMessage(u: UpdateCheck): String = when (u) {
    UpdateCheck.Idle -> ""
    UpdateCheck.Checking -> stringResource(R.string.updates_checking)
    is UpdateCheck.Available -> stringResource(R.string.updates_available, u.release.tag)
    UpdateCheck.UpToDate -> stringResource(R.string.updates_up_to_date)
    UpdateCheck.NoReleases -> stringResource(R.string.updates_none_published)
    is UpdateCheck.Failed -> if (u.offline) stringResource(R.string.offline_hint) else stringResource(R.string.updates_failed)
}
