package com.noor.app.ui.qibla

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noor.app.R
import com.noor.app.ui.appContainer
import com.noor.app.ui.components.GlassCard
import com.noor.app.ui.components.NoorTopBar
import com.noor.app.ui.components.UnavailableState
import com.noor.app.ui.components.octagram
import com.noor.app.ui.isArabic
import com.noor.app.ui.navigation.NoorNavigator
import com.noor.app.ui.navigation.Routes
import com.noor.app.ui.theme.LocalNoorPalette
import com.noor.app.util.Formatters
import com.noor.core.qibla.Qibla
import kotlin.math.abs
import kotlin.math.roundToInt

/** Device heading (degrees from true north) from the rotation-vector sensor; null when unavailable. */
@Composable
private fun rememberHeading(latitude: Double, longitude: Double): Pair<androidx.compose.runtime.State<Float>, Boolean> {
    val context = LocalContext.current
    val heading = remember { mutableFloatStateOf(0f) }
    val manager = remember { context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager }
    val sensor = remember { manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }
    val declination = remember(latitude, longitude) {
        GeomagneticField(latitude.toFloat(), longitude.toFloat(), 0f, System.currentTimeMillis()).declination
    }
    DisposableEffect(sensor, declination) {
        if (manager == null || sensor == null) return@DisposableEffect onDispose { }
        val rotation = FloatArray(9)
        val orientation = FloatArray(3)
        var smoothed = Float.NaN
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                SensorManager.getOrientation(rotation, orientation)
                val magnetic = Math.toDegrees(orientation[0].toDouble()).toFloat()
                val trueHeading = (magnetic + declination + 360f) % 360f
                // Low-pass filter on the circle to keep the needle calm.
                smoothed = if (smoothed.isNaN()) trueHeading else {
                    val d = Qibla.delta(smoothed.toDouble(), trueHeading.toDouble()).toFloat()
                    (smoothed + d * 0.15f + 360f) % 360f
                }
                heading.floatValue = smoothed
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { manager.unregisterListener(listener) }
    }
    return heading to (sensor != null)
}

@Composable
fun QiblaScreen(nav: NoorNavigator) {
    val container = appContainer()
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    val arabic = isArabic()
    Column(Modifier.fillMaxSize()) {
        NoorTopBar(stringResource(R.string.module_qibla), onBack = nav::back)
        val s = settings ?: return@Column
        val location = container.prayer.location(s)
        if (location == null) {
            UnavailableState(
                icon = Icons.Outlined.Explore,
                title = stringResource(R.string.prayer_no_location_title),
                message = stringResource(R.string.qibla_need_location),
                modifier = Modifier.padding(18.dp),
                actionLabel = stringResource(R.string.prayer_choose_city),
                onAction = { nav.open(Routes.CITY) },
            )
            return@Column
        }
        val lat = location.coordinates.latitude
        val lng = location.coordinates.longitude
        val bearing = remember(lat, lng) { Qibla.bearing(lat, lng).toFloat() }
        val distance = remember(lat, lng) { Qibla.distanceKm(lat, lng) }
        val (headingState, hasSensor) = rememberHeading(lat, lng)
        val offset by remember { derivedStateOf { Qibla.delta(headingState.value.toDouble(), bearing.toDouble()).roundToInt() } }
        val aligned = hasSensor && abs(offset) <= 3
        val haptics = LocalHapticFeedback.current
        LaunchedEffect(aligned) { if (aligned) haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
        val palette = LocalNoorPalette.current
        val onSurface = MaterialTheme.colorScheme.onSurfaceVariant

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(location.name(arabic), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            val description = stringResource(R.string.qibla_a11y, Formatters.number(bearing.roundToInt(), arabic))
            Box(
                Modifier
                    .widthIn(max = 340.dp)
                    .fillMaxWidth(0.86f)
                    .aspectRatio(1f)
                    .semantics { contentDescription = description },
                contentAlignment = Alignment.Center,
            ) {
                // Dial rotates opposite the heading (draw-phase only, no recomposition per sensor event).
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = if (hasSensor) -headingState.value else 0f },
                ) {
                    val r = size.minDimension / 2
                    drawCircle(onSurface.copy(alpha = 0.25f), r - 2.dp.toPx(), style = Stroke(1.5.dp.toPx()))
                    for (i in 0 until 72) {
                        val major = i % 18 == 0
                        rotate(i * 5f) {
                            drawLine(
                                if (major) palette.accent else onSurface.copy(alpha = 0.4f),
                                Offset(center.x, center.y - r + 6.dp.toPx()),
                                Offset(center.x, center.y - r + (if (major) 22 else 12).dp.toPx()),
                                strokeWidth = (if (major) 2.5f else 1f).dp.toPx(),
                                cap = StrokeCap.Round,
                            )
                        }
                    }
                    // North marker.
                    val n = Path().apply {
                        moveTo(center.x, center.y - r + 26.dp.toPx())
                        lineTo(center.x - 8.dp.toPx(), center.y - r + 42.dp.toPx())
                        lineTo(center.x + 8.dp.toPx(), center.y - r + 42.dp.toPx())
                        close()
                    }
                    drawPath(n, palette.accent)
                    // Qibla needle with a Kaaba-star at its tip.
                    rotate(bearing) {
                        drawLine(palette.heroAccent, center, Offset(center.x, center.y - r * 0.62f), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
                        drawPath(octagram(Offset(center.x, center.y - r * 0.70f), 14.dp.toPx(), 11.dp.toPx()), palette.heroAccent)
                    }
                    drawCircle(palette.heroAccent, 6.dp.toPx())
                }
            }
            GlassCard(Modifier.fillMaxWidth()) {
                Text(
                    when {
                        !hasSensor -> stringResource(R.string.qibla_no_sensor)
                        aligned -> stringResource(R.string.qibla_aligned)
                        offset > 0 -> stringResource(R.string.qibla_turn_right, Formatters.number(offset, arabic))
                        else -> stringResource(R.string.qibla_turn_left, Formatters.number(-offset, arabic))
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (aligned) palette.accent else MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                Row {
                    Text(stringResource(R.string.qibla_bearing, Formatters.number(bearing.roundToInt(), arabic)), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.qibla_distance, Formatters.number(distance.roundToInt(), arabic)), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text(stringResource(R.string.qibla_calibrate), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
        }
    }
}
