package com.noor.app.ui.tasbih

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.noor.app.AppContainer
import com.noor.app.R
import com.noor.app.data.db.TasbihEntity
import com.noor.app.ui.components.GlassCard
import com.noor.app.ui.components.NoorTopBar
import com.noor.app.ui.components.ProgressRing
import com.noor.app.ui.components.StarPattern
import com.noor.app.ui.isArabic
import com.noor.app.ui.navigation.NoorNavigator
import com.noor.app.ui.noorViewModel
import com.noor.app.ui.theme.LocalNoorPalette
import com.noor.app.ui.theme.NoorFonts
import com.noor.app.util.Formatters
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class Dhikr(val arabic: String, val english: String)

object Adhkar {
    val free = listOf(
        Dhikr("سُبْحَانَ اللهِ", "Subhan Allah"),
        Dhikr("الْحَمْدُ لِلَّهِ", "Alhamdulillah"),
        Dhikr("اللهُ أَكْبَرُ", "Allahu Akbar"),
        Dhikr("لَا إِلٰهَ إِلَّا اللهُ", "La ilaha illa Allah"),
        Dhikr("أَسْتَغْفِرُ اللهَ", "Astaghfirullah"),
        Dhikr("اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ وَآلِ مُحَمَّدٍ", "Salawat"),
    )

    /** Tasbih of Lady Fatima al-Zahra (a): 34 Takbir, 33 Tahmid, 33 Tasbih. */
    val zahra = listOf(
        Dhikr("اللهُ أَكْبَرُ", "Allahu Akbar") to 34,
        Dhikr("الْحَمْدُ لِلَّهِ", "Alhamdulillah") to 33,
        Dhikr("سُبْحَانَ اللهِ", "Subhan Allah") to 33,
    )
    val targets = listOf(33, 99, 100, 0)
}

enum class TasbihMode { ZAHRA, FREE }

data class TasbihUi(
    val mode: TasbihMode,
    val index: Int,
    val count: Int,
    val target: Int,
    val rounds: Int,
    val total: Long,
) {
    val zahraComplete: Boolean get() = mode == TasbihMode.ZAHRA && index >= Adhkar.zahra.size
    val dhikr: Dhikr? get() = when (mode) {
        TasbihMode.ZAHRA -> Adhkar.zahra.getOrNull(index)?.first
        TasbihMode.FREE -> Adhkar.free.getOrNull(index)
    }
    val effectiveTarget: Int get() = when (mode) {
        TasbihMode.ZAHRA -> Adhkar.zahra.getOrNull(index)?.second ?: 0
        TasbihMode.FREE -> target
    }
    val progress: Float get() = if (effectiveTarget <= 0) (count % 100) / 100f else count.toFloat() / effectiveTarget

    fun toEntity() = TasbihEntity(1, mode.name, index, count, target, rounds, total)

    companion object {
        val Default = TasbihUi(TasbihMode.ZAHRA, 0, 0, 33, 0, 0)
        fun from(e: TasbihEntity?) = if (e == null) Default else TasbihUi(
            runCatching { TasbihMode.valueOf(e.mode) }.getOrDefault(TasbihMode.ZAHRA),
            e.dhikrIndex, e.count, e.target, e.rounds, e.lifetimeTotal,
        )
    }
}

/** Result of a tap, used for haptic feedback. */
enum class TapResult { COUNTED, TARGET_REACHED, IGNORED }

/**
 * The in-memory state is authoritative so rapid taps are never lost; every change is
 * persisted to Room through a conflated channel (only the latest state is written).
 */
class TasbihViewModel(container: AppContainer) : ViewModel() {
    private val dao = container.database.tasbihDao()
    private val _state = MutableStateFlow<TasbihUi?>(null)
    val state: StateFlow<TasbihUi?> = _state.asStateFlow()
    private val writes = Channel<TasbihUi>(Channel.CONFLATED)

    init {
        viewModelScope.launch { _state.value = TasbihUi.from(dao.observe().first()) }
        viewModelScope.launch {
            for (ui in writes) dao.upsert(ui.toEntity())
        }
    }

    private fun save(ui: TasbihUi) {
        _state.value = ui
        writes.trySend(ui)
    }

    /** Taps before the saved state has loaded are ignored, so a load can never overwrite them. */
    fun tap(): TapResult {
        val s = state.value ?: return TapResult.IGNORED
        if (s.zahraComplete) return TapResult.IGNORED
        val count = s.count + 1
        val target = s.effectiveTarget
        val reached = target > 0 && count >= target
        val next = when {
            !reached -> s.copy(count = count, total = s.total + 1)
            s.mode == TasbihMode.ZAHRA -> {
                val nextIndex = s.index + 1
                s.copy(index = nextIndex, count = 0, total = s.total + 1, rounds = if (nextIndex >= Adhkar.zahra.size) s.rounds + 1 else s.rounds)
            }
            else -> s.copy(count = 0, rounds = s.rounds + 1, total = s.total + 1)
        }
        save(next)
        return if (reached) TapResult.TARGET_REACHED else TapResult.COUNTED
    }

    fun reset() {
        val s = state.value ?: return
        save(s.copy(count = 0, index = if (s.mode == TasbihMode.ZAHRA) 0 else s.index))
    }

    fun setMode(mode: TasbihMode) {
        val s = state.value ?: return
        if (s.mode != mode) save(s.copy(mode = mode, index = 0, count = 0))
    }

    fun selectDhikr(index: Int) {
        val s = state.value ?: return
        save(s.copy(index = index, count = 0))
    }

    fun setTarget(target: Int) {
        val s = state.value ?: return
        save(s.copy(target = target, count = 0))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihScreen(nav: NoorNavigator) {
    val vm = noorViewModel { TasbihViewModel(it) }
    val st by vm.state.collectAsStateWithLifecycle()
    val s = st ?: TasbihUi.Default
    val arabic = isArabic()
    val palette = LocalNoorPalette.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val bump = remember { Animatable(1f) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        NoorTopBar(stringResource(R.string.module_tasbih), onBack = nav::back, actions = {
            IconButton(onClick = { confirmReset = true }) { Icon(Icons.Outlined.Refresh, stringResource(R.string.tasbih_reset)) }
        })
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                TasbihMode.entries.forEachIndexed { i, mode ->
                    SegmentedButton(
                        selected = s.mode == mode,
                        onClick = { vm.setMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(i, TasbihMode.entries.size),
                        colors = SegmentedButtonDefaults.colors(activeContainerColor = palette.accent.copy(alpha = 0.2f), activeContentColor = palette.accent),
                    ) {
                        Text(stringResource(if (mode == TasbihMode.ZAHRA) R.string.tasbih_mode_zahra else R.string.tasbih_mode_free))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            if (s.mode == TasbihMode.FREE) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                    itemsIndexed(Adhkar.free) { i, d ->
                        FilterChip(
                            selected = s.index == i,
                            onClick = { vm.selectDhikr(i) },
                            label = { Text(if (arabic) d.arabic else d.english) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = palette.accent.copy(alpha = 0.2f), selectedLabelColor = palette.accent),
                        )
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                    itemsIndexed(Adhkar.targets) { _, t ->
                        FilterChip(
                            selected = s.target == t,
                            onClick = { vm.setTarget(t) },
                            label = { Text(if (t == 0) stringResource(R.string.tasbih_unlimited) else Formatters.number(t, arabic)) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = palette.accent.copy(alpha = 0.2f), selectedLabelColor = palette.accent),
                        )
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Adhkar.zahra.forEachIndexed { i, (d, n) ->
                        val done = s.index > i
                        val active = s.index == i
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(if (active) palette.accent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                "${if (done) "✓ " else ""}${if (arabic) d.arabic else d.english} · ${Formatters.number(n, arabic)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active || done) palette.accent else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            AnimatedContent(
                targetState = s.dhikr?.arabic ?: stringResource(R.string.tasbih_complete),
                transitionSpec = { (fadeIn(tween(300)) + scaleIn(initialScale = 0.9f)) togetherWith fadeOut(tween(200)) },
                label = "dhikr",
            ) { text ->
                Text(
                    text,
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = NoorFonts.Naskh, fontSize = 30.sp, lineHeight = 48.sp),
                    color = palette.accent,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(18.dp))

            // The counter: tap anywhere on the circle. Shown once the saved count has loaded.
            val interaction = remember { MutableInteractionSource() }
            if (st == null) {
                com.noor.app.ui.components.ShimmerBlock(
                    Modifier.widthIn(max = 320.dp).fillMaxWidth(0.82f).aspectRatio(1f),
                    shape = CircleShape,
                )
            } else Box(
                Modifier
                    .widthIn(max = 320.dp)
                    .fillMaxWidth(0.82f)
                    .aspectRatio(1f)
                    .scale(bump.value)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(palette.heroStart, palette.heroEnd)))
                    .border(1.dp, palette.heroAccent.copy(alpha = 0.5f), CircleShape)
                    .clickable(interactionSource = interaction, indication = androidx.compose.foundation.LocalIndication.current, onClickLabel = stringResource(R.string.tasbih_tap_hint)) {
                        when (vm.tap()) {
                            TapResult.TARGET_REACHED -> haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            TapResult.COUNTED -> haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            TapResult.IGNORED -> Unit
                        }
                        scope.launch {
                            bump.snapTo(0.96f)
                            bump.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                StarPattern(Modifier.fillMaxSize(0.92f), color = palette.heroAccent.copy(alpha = 0.08f))
                ProgressRing(
                    progress = if (s.zahraComplete) 1f else s.progress,
                    modifier = Modifier.fillMaxSize(0.86f),
                    color = palette.heroAccent,
                    trackColor = palette.onHero.copy(alpha = 0.12f),
                    strokeWidth = 8.dp,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedContent(
                        targetState = s.count,
                        transitionSpec = { (fadeIn(tween(150)) + scaleIn(initialScale = 0.8f)) togetherWith fadeOut(tween(100)) },
                        label = "count",
                    ) { c ->
                        Text(
                            Formatters.number(c, arabic),
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp),
                            color = palette.onHero,
                        )
                    }
                    val t = s.effectiveTarget
                    Text(
                        if (s.zahraComplete) stringResource(R.string.tasbih_tap_to_restart_hint)
                        else if (t > 0) stringResource(R.string.tasbih_of_target, Formatters.number(t, arabic))
                        else stringResource(R.string.tasbih_tap_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.onHero.copy(alpha = 0.75f),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Stat(stringResource(R.string.tasbih_rounds), Formatters.number(s.rounds, arabic), Modifier.weight(1f))
                Stat(stringResource(R.string.tasbih_total), Formatters.number(s.total, arabic), Modifier.weight(1f))
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.tasbih_reset)) },
            text = { Text(stringResource(R.string.tasbih_reset_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.reset()
                    confirmReset = false
                }) { Text(stringResource(R.string.tasbih_reset)) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    GlassCard(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge, color = LocalNoorPalette.current.accent)
    }
}
