package com.noor.core.qibla

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object Qibla {
    /** Coordinates of the Holy Kaaba. */
    const val KAABA_LAT = 21.422487
    const val KAABA_LNG = 39.826206

    /** Initial great-circle bearing from the given point to the Kaaba, degrees clockwise from true north (0..360). */
    fun bearing(latitude: Double, longitude: Double): Double {
        val phi1 = Math.toRadians(latitude)
        val phi2 = Math.toRadians(KAABA_LAT)
        val dLambda = Math.toRadians(KAABA_LNG - longitude)
        val y = sin(dLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLambda)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    /** Great-circle distance to the Kaaba in kilometres (haversine). */
    fun distanceKm(latitude: Double, longitude: Double): Double {
        val r = 6371.0088
        val dPhi = Math.toRadians(KAABA_LAT - latitude)
        val dLambda = Math.toRadians(KAABA_LNG - longitude)
        val a = sin(dPhi / 2) * sin(dPhi / 2) +
            cos(Math.toRadians(latitude)) * cos(Math.toRadians(KAABA_LAT)) * sin(dLambda / 2) * sin(dLambda / 2)
        return 2 * r * atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }

    /** Signed smallest rotation from [from] to [to] in degrees (-180..180]. */
    fun delta(from: Double, to: Double): Double {
        var d = (to - from) % 360.0
        if (d > 180) d -= 360.0
        if (d <= -180) d += 360.0
        return d
    }
}
