package com.noor.app.data.prayer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Optional, permission-gated coarse location using the platform LocationManager
 * (no Google Play Services dependency).
 */
class LocationProvider(private val context: Context) {
    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): Location? {
        if (!hasPermission()) return null
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER)
            .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        val recent = providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
        if (recent != null && System.currentTimeMillis() - recent.time < MAX_AGE_MS) return recent
        val provider = providers.firstOrNull { it != LocationManager.PASSIVE_PROVIDER } ?: return recent
        return withTimeoutOrNull(TIMEOUT_MS) { requestSingle(provider) } ?: recent
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingle(provider: String): Location? = suspendCancellableCoroutine { cont ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val signal = android.os.CancellationSignal()
            cont.invokeOnCancellation { signal.cancel() }
            manager.getCurrentLocation(provider, signal, ContextCompat.getMainExecutor(context)) { location ->
                if (cont.isActive) cont.resume(location)
            }
        } else {
            // All methods are implemented: before API 30 they are abstract on the device.
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    manager.removeUpdates(this)
                    if (cont.isActive) cont.resume(location)
                }

                override fun onProviderEnabled(provider: String) = Unit

                override fun onProviderDisabled(provider: String) = Unit

                @Deprecated("Deprecated in API 29")
                @Suppress("DEPRECATION")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            }
            cont.invokeOnCancellation { manager.removeUpdates(listener) }
            manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
        }
    }

    companion object {
        private const val MAX_AGE_MS = 30 * 60 * 1000L
        private const val TIMEOUT_MS = 15_000L
    }
}
