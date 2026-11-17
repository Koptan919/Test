package com.noor.app.ui.audio

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noor.app.R
import com.noor.app.data.db.AudioTrackEntity
import com.noor.app.ui.appContainer
import com.noor.app.ui.components.GlassCard
import com.noor.app.ui.components.IconBadge
import com.noor.app.ui.components.NoorDialog
import com.noor.app.ui.components.NoorTopBar
import com.noor.app.ui.components.UnavailableState
import com.noor.app.ui.isArabic
import com.noor.app.ui.navigation.NoorNavigator
import com.noor.app.ui.theme.LocalNoorPalette
import com.noor.app.util.Formatters
import kotlinx.coroutines.launch

private fun mmss(ms: Long, arabic: Boolean): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return Formatters.digits("%d:%02d".format(java.util.Locale.ENGLISH, s / 60, s % 60), arabic)
}

@Composable
fun AudioScreen(nav: NoorNavigator) {
    val container = appContainer()
    val library = container.audio
    val tracks by library.tracks.collectAsStateWithLifecycle(initialValue = null)
    val player by library.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val arabic = isArabic()
    val palette = LocalNoorPalette.current
    var pendingDelete by remember { mutableStateOf<AudioTrackEntity?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) scope.launch { library.add(uris) }
    }

    Column(Modifier.fillMaxSize()) {
        NoorTopBar(stringResource(R.string.module_audio), onBack = nav::back)
        val list = tracks
        Column(Modifier.weight(1f)) {
            if (list != null && list.isEmpty()) {
                UnavailableState(
                    icon = Icons.Outlined.Headphones,
                    title = stringResource(R.string.audio_empty_title),
                    message = stringResource(R.string.audio_empty_msg),
                    modifier = Modifier.padding(18.dp),
                    actionLabel = stringResource(R.string.audio_add),
                    onAction = { picker.launch(arrayOf("audio/*")) },
                )
            } else if (list != null) {
                LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(list, key = { it.uri }) { track ->
                        val current = player.current?.uri == track.uri
                        GlassCard(
                            Modifier.fillMaxWidth().animateItem(),
                            onClick = { library.play(track) },
                            onClickLabel = stringResource(if (current && player.playing) R.string.audio_pause else R.string.audio_play),
                            contentPadding = PaddingValues(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconBadge(if (current && player.playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, size = 40.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, color = if (current) palette.accent else MaterialTheme.colorScheme.onSurface)
                                    if (track.durationMs > 0) Text(mmss(track.durationMs, arabic), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { pendingDelete = track }) {
                                    Icon(Icons.Outlined.Delete, stringResource(R.string.action_remove), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    item { Text(stringResource(R.string.audio_licence_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
        AnimatedVisibility(visible = player.current != null, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
            val current = player.current
            GlassCard(Modifier.fillMaxWidth().padding(12.dp)) {
                Text(current?.title.orEmpty(), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (player.error) Text(stringResource(R.string.audio_error), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                Slider(
                    value = player.positionMs.toFloat(),
                    onValueChange = { library.seekTo(it.toLong()) },
                    valueRange = 0f..maxOf(1f, player.durationMs.toFloat()),
                    colors = SliderDefaults.colors(thumbColor = palette.accent, activeTrackColor = palette.accent),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${mmss(player.positionMs, arabic)} / ${mmss(player.durationMs, arabic)}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = library::stop) { Icon(Icons.Outlined.Stop, stringResource(R.string.audio_stop)) }
                    FilledIconButton(
                        onClick = library::toggle,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = palette.accent, contentColor = MaterialTheme.colorScheme.background),
                    ) {
                        Icon(
                            if (player.playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            stringResource(if (player.playing) R.string.audio_pause else R.string.audio_play),
                        )
                    }
                }
            }
        }
        if (list?.isNotEmpty() == true && player.current == null) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
                ExtendedFloatingActionButton(
                    onClick = { picker.launch(arrayOf("audio/*")) },
                    icon = { Icon(Icons.Outlined.Add, null) },
                    text = { Text(stringResource(R.string.audio_add)) },
                    containerColor = palette.accent,
                    contentColor = MaterialTheme.colorScheme.background,
                )
            }
        }
    }
    pendingDelete?.let { t ->
        NoorDialog(
            title = stringResource(R.string.audio_remove_title),
            text = stringResource(R.string.audio_remove_msg, t.title),
            confirmLabel = stringResource(R.string.action_remove),
            onConfirm = {
                scope.launch { library.remove(t) }
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}
