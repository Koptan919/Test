package com.noor.core.calendar

enum class OccasionType { MARTYRDOM, BIRTH, EID, SACRED_NIGHT, HISTORIC }

/**
 * A religious occasion in the Hijri calendar as commonly observed by Twelver Shia
 * communities. [lastDayOfMonth] marks occasions observed on the final day of the month.
 * Some dates have more than one narration; the app shows a notice to that effect.
 */
data class Occasion(
    val month: Int,
    val day: Int,
    val type: OccasionType,
    val titleAr: String,
    val titleEn: String,
    val lastDayOfMonth: Boolean = false,
) {
    fun title(arabic: Boolean) = if (arabic) titleAr else titleEn

    fun observedDay(monthLength: Int): Int = if (lastDayOfMonth) monthLength else day
}

object Occasions {
    val all: List<Occasion> = listOf(
        Occasion(1, 1, OccasionType.HISTORIC, "بداية السنة الهجرية", "Beginning of the Hijri year"),
        Occasion(1, 10, OccasionType.MARTYRDOM, "يوم عاشوراء – استشهاد الإمام الحسين (ع)", "Ashura – Martyrdom of Imam al-Husayn (a)"),
        Occasion(2, 20, OccasionType.HISTORIC, "زيارة الأربعين", "Arba'een"),
        Occasion(2, 28, OccasionType.MARTYRDOM, "وفاة النبي محمد (ص) واستشهاد الإمام الحسن (ع)", "Demise of the Prophet (s) and martyrdom of Imam al-Hasan (a)"),
        Occasion(2, 30, OccasionType.MARTYRDOM, "استشهاد الإمام علي بن موسى الرضا (ع)", "Martyrdom of Imam Ali al-Rida (a)", lastDayOfMonth = true),
        Occasion(3, 8, OccasionType.MARTYRDOM, "استشهاد الإمام الحسن العسكري (ع)", "Martyrdom of Imam al-Hasan al-Askari (a)"),
        Occasion(3, 17, OccasionType.BIRTH, "مولد النبي محمد (ص) والإمام جعفر الصادق (ع)", "Birth of the Prophet Muhammad (s) and Imam Ja'far al-Sadiq (a)"),
        Occasion(4, 8, OccasionType.BIRTH, "مولد الإمام الحسن العسكري (ع)", "Birth of Imam al-Hasan al-Askari (a)"),
        Occasion(5, 13, OccasionType.MARTYRDOM, "شهادة السيدة فاطمة الزهراء (ع) – رواية ٧٥ يوماً", "Martyrdom of Lady Fatima al-Zahra (a) – 75-day narration"),
        Occasion(6, 3, OccasionType.MARTYRDOM, "شهادة السيدة فاطمة الزهراء (ع) – رواية ٩٥ يوماً", "Martyrdom of Lady Fatima al-Zahra (a) – 95-day narration"),
        Occasion(6, 20, OccasionType.BIRTH, "مولد السيدة فاطمة الزهراء (ع)", "Birth of Lady Fatima al-Zahra (a)"),
        Occasion(7, 1, OccasionType.BIRTH, "مولد الإمام محمد الباقر (ع)", "Birth of Imam Muhammad al-Baqir (a)"),
        Occasion(7, 3, OccasionType.MARTYRDOM, "استشهاد الإمام علي الهادي (ع)", "Martyrdom of Imam Ali al-Hadi (a)"),
        Occasion(7, 10, OccasionType.BIRTH, "مولد الإمام محمد الجواد (ع)", "Birth of Imam Muhammad al-Jawad (a)"),
        Occasion(7, 13, OccasionType.BIRTH, "مولد الإمام علي بن أبي طالب (ع)", "Birth of Imam Ali ibn Abi Talib (a)"),
        Occasion(7, 25, OccasionType.MARTYRDOM, "استشهاد الإمام موسى الكاظم (ع)", "Martyrdom of Imam Musa al-Kazim (a)"),
        Occasion(7, 27, OccasionType.EID, "المبعث النبوي الشريف", "Al-Mab'ath – the Prophetic mission"),
        Occasion(8, 3, OccasionType.BIRTH, "مولد الإمام الحسين (ع)", "Birth of Imam al-Husayn (a)"),
        Occasion(8, 4, OccasionType.BIRTH, "مولد أبي الفضل العباس (ع)", "Birth of Abu al-Fadl al-Abbas (a)"),
        Occasion(8, 5, OccasionType.BIRTH, "مولد الإمام علي زين العابدين (ع)", "Birth of Imam Ali Zayn al-Abidin (a)"),
        Occasion(8, 15, OccasionType.BIRTH, "مولد الإمام المهدي (عج)", "Birth of Imam al-Mahdi (aj)"),
        Occasion(9, 1, OccasionType.HISTORIC, "بداية شهر رمضان المبارك", "Beginning of the holy month of Ramadan"),
        Occasion(9, 10, OccasionType.MARTYRDOM, "وفاة السيدة خديجة الكبرى (ع)", "Demise of Lady Khadija al-Kubra (a)"),
        Occasion(9, 15, OccasionType.BIRTH, "مولد الإمام الحسن المجتبى (ع)", "Birth of Imam al-Hasan al-Mujtaba (a)"),
        Occasion(9, 19, OccasionType.SACRED_NIGHT, "ليلة القدر (الليلة ١٩) – ضربة الإمام علي (ع)", "Laylat al-Qadr (19th night) – Imam Ali (a) struck"),
        Occasion(9, 21, OccasionType.MARTYRDOM, "استشهاد الإمام علي (ع) – ليلة القدر (الليلة ٢١)", "Martyrdom of Imam Ali (a) – Laylat al-Qadr (21st night)"),
        Occasion(9, 23, OccasionType.SACRED_NIGHT, "ليلة القدر (الليلة ٢٣)", "Laylat al-Qadr (23rd night)"),
        Occasion(10, 1, OccasionType.EID, "عيد الفطر", "Eid al-Fitr"),
        Occasion(10, 25, OccasionType.MARTYRDOM, "استشهاد الإمام جعفر الصادق (ع)", "Martyrdom of Imam Ja'far al-Sadiq (a)"),
        Occasion(11, 11, OccasionType.BIRTH, "مولد الإمام علي بن موسى الرضا (ع)", "Birth of Imam Ali al-Rida (a)"),
        Occasion(11, 30, OccasionType.MARTYRDOM, "استشهاد الإمام محمد الجواد (ع)", "Martyrdom of Imam Muhammad al-Jawad (a)", lastDayOfMonth = true),
        Occasion(12, 1, OccasionType.HISTORIC, "زواج الإمام علي والسيدة فاطمة (ع)", "Marriage of Imam Ali and Lady Fatima (a)"),
        Occasion(12, 7, OccasionType.MARTYRDOM, "استشهاد الإمام محمد الباقر (ع)", "Martyrdom of Imam Muhammad al-Baqir (a)"),
        Occasion(12, 9, OccasionType.SACRED_NIGHT, "يوم عرفة", "Day of Arafah"),
        Occasion(12, 10, OccasionType.EID, "عيد الأضحى", "Eid al-Adha"),
        Occasion(12, 15, OccasionType.BIRTH, "مولد الإمام علي الهادي (ع)", "Birth of Imam Ali al-Hadi (a)"),
        Occasion(12, 18, OccasionType.EID, "عيد الغدير الأغر", "Eid al-Ghadir"),
        Occasion(12, 24, OccasionType.HISTORIC, "يوم المباهلة", "Day of Mubahala"),
    )

    fun forMonth(month: Int, monthLength: Int): List<Pair<Int, Occasion>> =
        all.filter { it.month == month }
            .map { it.observedDay(monthLength) to it }
            .sortedBy { it.first }

    fun on(date: HijriDate, converter: HijriConverter): List<Occasion> {
        val length = converter.monthLength(date.year, date.month)
        return all.filter { it.month == date.month && it.observedDay(length) == date.day }
    }
}
