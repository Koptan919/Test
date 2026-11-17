package com.noor.core.calendar

/**
 * Seasons that may tint the app's atmosphere. Windows are computed from the
 * (user-adjusted) Hijri date; because month starts depend on moon sighting,
 * each window is padded so a one-day difference does not cause flicker at the edges.
 */
enum class Season { NONE, RAMADAN, MUHARRAM, ARBAEEN }

object Seasons {
    fun of(date: HijriDate): Season = when {
        date.month == 9 -> Season.RAMADAN
        // First thirteen days of Muharram (mourning of Imam al-Husayn (a) through the 12th–13th).
        date.month == 1 && date.day <= 13 -> Season.MUHARRAM
        // The ten days leading to and including Arba'een (20 Safar).
        date.month == 2 && date.day in 10..20 -> Season.ARBAEEN
        else -> Season.NONE
    }
}
