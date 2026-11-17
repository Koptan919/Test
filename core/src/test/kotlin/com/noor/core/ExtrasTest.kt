package com.noor.core

import com.noor.core.calendar.HijriDate
import com.noor.core.calendar.Season
import com.noor.core.calendar.Seasons
import com.noor.core.qibla.Qibla
import com.noor.core.update.Version
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtrasTest {
    @Test
    fun qiblaBearingsMatchKnownValues() {
        // Reference bearings (true north) widely published for these cities.
        assertEquals(119.0, Qibla.bearing(51.5074, -0.1278), 0.5) // London
        assertEquals(58.5, Qibla.bearing(40.7128, -74.0060), 0.5) // New York
        assertEquals(0.0, Qibla.distanceKm(Qibla.KAABA_LAT, Qibla.KAABA_LNG), 0.001)
        assertEquals(-20.0, Qibla.delta(10.0, 350.0), 1e-9)
        assertEquals(20.0, Qibla.delta(350.0, 10.0), 1e-9)
    }

    @Test
    fun seasons() {
        assertEquals(Season.RAMADAN, Seasons.of(HijriDate(1447, 9, 15)))
        assertEquals(Season.MUHARRAM, Seasons.of(HijriDate(1448, 1, 10)))
        assertEquals(Season.NONE, Seasons.of(HijriDate(1448, 1, 20)))
        assertEquals(Season.ARBAEEN, Seasons.of(HijriDate(1448, 2, 20)))
        assertEquals(Season.NONE, Seasons.of(HijriDate(1448, 2, 21)))
    }

    @Test
    fun versions() {
        assertTrue(Version.isNewer("v0.2.0", "0.1.0"))
        assertTrue(Version.isNewer("1.0", "0.9.9"))
        assertFalse(Version.isNewer("v0.1.0", "0.1.0"))
        assertFalse(Version.isNewer("v0.0.9", "0.1.0"))
        assertFalse(Version.isNewer("garbage", "0.1.0"))
        assertNull(Version.parse("x1"))
        assertEquals(Version(1, 2, 3), Version.parse("v1.2.3-beta"))
    }
}
