package com.noor.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.noor.app.R
import com.noor.app.ui.theme.LocalNoorPalette
import com.noor.app.ui.theme.LocalReduceMotion
import kotlin.math.cos
import kotlin.math.sin

/** Full-screen background with a single soft ambient glow (static when motion is reduced). */
@Composable
fun NoorBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val palette = LocalNoorPalette.current
    val reduce = LocalReduceMotion.current
    val drift = if (reduce) {
        0.5f
    } else {
        val transition = rememberInfiniteTransition(label = "ambient")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Reverse),
            label = "drift",
        ).value
    }
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                // Drawn in the draw phase only: the animation never triggers recomposition.
                val c = Offset(size.width * (0.2f + 0.3f * drift), size.height * 0.1f)
                drawCircle(
                    brush = Brush.radialGradient(listOf(palette.ambient, Color.Transparent), center = c, radius = size.width),
                    radius = size.width,
                    center = c,
                )
            },
    ) {
        // Transparent containers above don't set a content colour; provide one so text is never black-on-dark.
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onBackground,
        ) { content() }
    }
}

/** Scale-down-on-press feedback shared by interactive surfaces (disabled when motion is reduced). */
@Composable
fun Modifier.pressScale(interaction: MutableInteractionSource): Modifier {
    if (LocalReduceMotion.current) return this
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 600f),
        label = "pressScale",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Click with a light haptic tick. */
@Composable
fun Modifier.noorClickable(
    interaction: MutableInteractionSource,
    onClickLabel: String? = null,
    onClick: () -> Unit,
): Modifier {
    val haptics = LocalHapticFeedback.current
    return this.clickable(
        interactionSource = interaction,
        indication = LocalIndication.current,
        onClickLabel = onClickLabel,
    ) {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onClick()
    }
}

/** Subtle "liquid glass" surface: translucent fill with a hairline border, no heavy blur. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    shape: Shape = RoundedCornerShape(24.dp),
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalNoorPalette.current
    val interaction = remember { MutableInteractionSource() }
    var m = modifier
        .then(if (onClick != null) Modifier.pressScale(interaction) else Modifier)
        .clip(shape)
        .background(palette.glassFill)
        .border(1.dp, palette.glassBorder, shape)
    if (onClick != null) m = m.noorClickable(interaction, onClickLabel, onClick)
    Column(m.padding(contentPadding), content = content)
}

/** Hero surface with a gentle "breathing" ambient light. */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalNoorPalette.current
    val reduce = LocalReduceMotion.current
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(28.dp)
    val glow = if (reduce) {
        0.5f
    } else {
        rememberInfiniteTransition(label = "hero").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(6_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "glow",
        ).value
    }
    var m = modifier
        .then(if (onClick != null) Modifier.pressScale(interaction) else Modifier)
        .clip(shape)
        .background(Brush.linearGradient(listOf(palette.heroStart, palette.heroEnd)))
        .drawBehind {
            val c = Offset(size.width * 0.85f, size.height * (0.1f + 0.15f * glow))
            drawCircle(
                Brush.radialGradient(listOf(palette.heroAccent.copy(alpha = 0.10f + 0.06f * glow), Color.Transparent), c, size.width * 0.6f),
                radius = size.width * 0.6f,
                center = c,
            )
        }
        .border(1.dp, palette.heroAccent.copy(alpha = 0.28f), shape)
    if (onClick != null) m = m.noorClickable(interaction, onClickLabel, onClick)
    Box(m) {
        StarPattern(
            Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .size(120.dp),
            color = palette.heroAccent.copy(alpha = 0.09f),
        )
        Column(Modifier.padding(22.dp), content = content)
    }
}

/** Decorative eight-pointed star rosette (slowly rotating unless motion is reduced). */
@Composable
fun StarPattern(modifier: Modifier = Modifier, color: Color) {
    val reduce = LocalReduceMotion.current
    val rotation = if (reduce) {
        0f
    } else {
        rememberInfiniteTransition(label = "star").animateFloat(
            initialValue = 0f,
            targetValue = 45f,
            animationSpec = infiniteRepeatable(tween(40_000, easing = LinearEasing), RepeatMode.Reverse),
            label = "rotation",
        ).value
    }
    Canvas(modifier.graphicsLayer { rotationZ = rotation }) {
        listOf(1f, 0.7f).forEach { k ->
            drawPath(octagram(center, size.minDimension / 2 * k, size.minDimension / 2 * k * 0.84f), color, style = Stroke(1.4.dp.toPx()))
        }
    }
}

fun octagram(center: Offset, outer: Float, inner: Float): Path = Path().apply {
    for (i in 0 until 16) {
        val r = if (i % 2 == 0) outer else inner
        val a = Math.toRadians(-90.0 + i * 22.5)
        val x = center.x + r * cos(a).toFloat()
        val y = center.y + r * sin(a).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/** Ayah number inside a small eight-pointed star. */
@Composable
fun AyahNumber(number: String, modifier: Modifier = Modifier, size: Dp = 34.dp) {
    val accent = LocalNoorPalette.current.accent
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawPath(octagram(center, this.size.minDimension / 2, this.size.minDimension / 2 * 0.86f), accent, style = Stroke(1.3.dp.toPx()))
        }
        Text(number, style = MaterialTheme.typography.labelSmall, color = accent, maxLines = 1)
    }
}

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color,
    trackColor: Color,
    strokeWidth: Dp = 6.dp,
) {
    val reduce = LocalReduceMotion.current
    val animated by animateFloatAsState(
        progress.coerceIn(0f, 1f),
        if (reduce) tween(0) else tween(700, easing = FastOutSlowInEasing),
        label = "ring",
    )
    Canvas(modifier) {
        val stroke = strokeWidth.toPx()
        drawArc(trackColor, 0f, 360f, false, style = Stroke(stroke))
        drawArc(color, -90f, 360f * animated, false, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 4.dp, height = 18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(LocalNoorPalette.current.accent),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel, color = LocalNoorPalette.current.accent) }
        }
    }
}

/** Large, readable shortcut tile with an icon badge. */
@Composable
fun ShortcutTile(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    GlassCard(modifier.heightIn(min = 112.dp), onClick = onClick, contentPadding = PaddingValues(16.dp)) {
        IconBadge(icon)
        Spacer(Modifier.height(12.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun IconBadge(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val palette = LocalNoorPalette.current
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(palette.heroStart)
            .border(1.dp, palette.heroAccent.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = palette.heroAccent, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun UnavailableState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    GlassCard(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            IconBadge(icon, size = 56.dp)
            Spacer(Modifier.height(14.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(6.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(14.dp))
                AccentButton(actionLabel, onAction)
            }
            if (secondaryLabel != null && onSecondary != null) {
                TextButton(onClick = onSecondary) { Text(secondaryLabel) }
            }
        }
    }
}

/** The primary action of a screen: solid champagne with dark text (high contrast in every theme). */
@Composable
fun AccentButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val palette = LocalNoorPalette.current
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = palette.heroAccent,
            contentColor = Color(0xFF101114),
        ),
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, maxLines = 1)
    }
}

/** Small pill shown when there is no connection; everything in Noor still works offline. */
@Composable
fun OfflineChip(modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.CloudOff, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.offline_chip), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Loading placeholder: a soft pulse (static when motion is reduced). */
@Composable
fun ShimmerBlock(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(18.dp)) {
    val reduce = LocalReduceMotion.current
    val alpha = if (reduce) {
        0.08f
    } else {
        rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue = 0.05f,
            targetValue = 0.12f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "alpha",
        ).value
    }
    val color = MaterialTheme.colorScheme.onSurface
    Box(
        modifier
            .clip(shape)
            .drawBehind { drawRect(color.copy(alpha = alpha)) },
    )
}

@Composable
fun LoadingList(modifier: Modifier = Modifier, count: Int = 6) {
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(count) { ShimmerBlock(Modifier.fillMaxWidth().height(if (it == 0) 120.dp else 72.dp)) }
    }
}

/** Refined confirmation dialog in the app's style. */
@Composable
fun NoorDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String = stringResource(R.string.action_cancel),
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(text, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel, color = LocalNoorPalette.current.accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissLabel) } },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(28.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoorTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
        ),
    )
}
