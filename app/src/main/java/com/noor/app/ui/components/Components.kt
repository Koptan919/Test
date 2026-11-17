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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.noor.app.R
import com.noor.app.ui.theme.LocalNoorPalette
import kotlin.math.cos
import kotlin.math.sin

/** Full-screen background with slowly drifting burgundy and gold glows behind the content. */
@Composable
fun NoorBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val palette = LocalNoorPalette.current
    val transition = rememberInfiniteTransition(label = "glow")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift",
    )
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val a = Offset(w * (0.15f + 0.25f * drift), h * (0.08f + 0.05f * drift))
            val b = Offset(w * (0.95f - 0.2f * drift), h * (0.45f + 0.1f * drift))
            drawCircle(
                brush = Brush.radialGradient(listOf(palette.glowA, Color.Transparent), center = a, radius = w * 0.9f),
                radius = w * 0.9f,
                center = a,
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(palette.glowB, Color.Transparent), center = b, radius = w * 0.7f),
                radius = w * 0.7f,
                center = b,
            )
        }
        content()
    }
}

/** Scale-down-on-press feedback shared by all interactive cards. */
@Composable
fun Modifier.pressScale(interaction: MutableInteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "pressScale",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Subtle "liquid glass" surface: translucent gradient fill with a gold-tinted hairline border. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(24.dp),
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalNoorPalette.current
    val interaction = remember { MutableInteractionSource() }
    val base = modifier
        .then(if (onClick != null) Modifier.pressScale(interaction) else Modifier)
        .clip(shape)
        .background(Brush.verticalGradient(listOf(palette.glassFillTop, palette.glassFillBottom)))
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(listOf(palette.glassBorderStrong, palette.glassBorderSoft, palette.glassBorderStrong.copy(alpha = 0.25f))),
            shape = shape,
        )
    val clickable = if (onClick != null) {
        base.clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick)
    } else base
    Column(clickable.padding(contentPadding), content = content)
}

/** Burgundy hero surface used for the most important card on a screen. */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalNoorPalette.current
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(28.dp)
    var m = modifier
        .then(if (onClick != null) Modifier.pressScale(interaction) else Modifier)
        .clip(shape)
        .background(Brush.linearGradient(listOf(palette.heroStart, palette.heroEnd)))
        .border(1.dp, Brush.linearGradient(listOf(palette.heroAccent.copy(alpha = 0.7f), Color.Transparent, palette.heroAccent.copy(alpha = 0.35f))), shape)
    if (onClick != null) m = m.clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick)
    Box(m) {
        StarPattern(
            Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(150.dp),
            color = palette.heroAccent.copy(alpha = 0.10f),
        )
        Column(Modifier.padding(22.dp), content = content)
    }
}

/** Decorative eight-pointed star rosette that slowly rotates. */
@Composable
fun StarPattern(modifier: Modifier = Modifier, color: Color) {
    val transition = rememberInfiniteTransition(label = "star")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 45f,
        animationSpec = infiniteRepeatable(tween(30_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "rotation",
    )
    Canvas(modifier.graphicsLayer { rotationZ = rotation }) {
        val c = center
        listOf(1f, 0.72f, 0.46f).forEach { k ->
            val path = octagram(c, size.minDimension / 2 * k, size.minDimension / 2 * k * 0.84f)
            drawPath(path, color, style = Stroke(width = 1.5.dp.toPx()))
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
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing), label = "ring")
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
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 4.dp, height = 18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(LocalNoorPalette.current.accent),
        )
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
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
    GlassCard(modifier, onClick = onClick, contentPadding = PaddingValues(16.dp)) {
        IconBadge(icon)
        Spacer(Modifier.height(12.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
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
            .background(Brush.linearGradient(listOf(palette.heroStart.copy(alpha = 0.9f), palette.heroEnd)))
            .border(1.dp, palette.heroAccent.copy(alpha = 0.5f), CircleShape),
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
) {
    GlassCard(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            IconBadge(icon, size = 56.dp)
            Spacer(Modifier.height(14.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(14.dp))
                FilledTonalButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

/** Animated shimmer placeholder for loading states. */
@Composable
fun ShimmerBlock(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(16.dp)) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
        label = "x",
    )
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val highlight = LocalNoorPalette.current.accent.copy(alpha = 0.16f)
    Box(
        modifier
            .clip(shape)
            .background(base)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color.Transparent, highlight, Color.Transparent),
                    start = Offset(x * 800f, 0f),
                    end = Offset(x * 800f + 400f, 400f),
                ),
            ),
    )
}

@Composable
fun LoadingList(modifier: Modifier = Modifier, count: Int = 6) {
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(count) { ShimmerBlock(Modifier.fillMaxWidth().height(if (it == 0) 120.dp else 72.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoorTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    CenterAlignedTopAppBar(
        title = {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
        ),
    )
}
