package com.noor.core.content

import com.noor.core.quran.AyahRef
import java.time.LocalDate

/**
 * Verses featured as "verse of the day". Only references are stored here; the text is
 * always read from the verified Quran asset.
 */
object DailyVerses {
    val refs: List<AyahRef> = listOf(
        AyahRef(2, 152), AyahRef(2, 186), AyahRef(2, 255), AyahRef(3, 139), AyahRef(3, 200),
        AyahRef(5, 55), AyahRef(13, 28), AyahRef(14, 7), AyahRef(17, 82), AyahRef(20, 114),
        AyahRef(24, 35), AyahRef(29, 69), AyahRef(33, 33), AyahRef(33, 56), AyahRef(39, 53),
        AyahRef(40, 60), AyahRef(42, 23), AyahRef(49, 13), AyahRef(55, 13), AyahRef(57, 4),
        AyahRef(65, 3), AyahRef(76, 8), AyahRef(93, 5), AyahRef(94, 5), AyahRef(94, 6),
        AyahRef(97, 1), AyahRef(103, 1), AyahRef(108, 1), AyahRef(112, 1), AyahRef(28, 5),
    )

    fun forDate(date: LocalDate): AyahRef = refs[Math.floorMod(date.toEpochDay(), refs.size.toLong()).toInt()]
}

enum class DuaCategory(val key: String) {
    QURANIC("quranic"),
    SUPPLICATIONS("supplications"),
    ZIYARAT("ziyarat"),
    MUNAJAT("munajat"),
    TASBIHAT("tasbihat"),
    SAHIFA("sahifa"),
    NAHJ("nahj"),
    ;

    /** Categories a verified content pack may add new entries to (books with many chapters). */
    val acceptsNewEntries: Boolean get() = this == SAHIFA || this == NAHJ || this == SUPPLICATIONS || this == ZIYARAT || this == MUNAJAT

    companion object {
        fun fromKey(key: String?): DuaCategory? = entries.firstOrNull { it.key == key }
    }
}

/**
 * A catalogue entry. Text is shown only when it comes from a verified source:
 * - [quranRefs]: resolved from the verified Quran text;
 * - [bundledText]: short formulas included verbatim;
 * - otherwise from a user-imported content pack. Without one, the app shows an
 *   "unavailable" state instead of unverified text.
 */
data class DuaEntry(
    val id: String,
    val category: DuaCategory,
    val titleAr: String,
    val titleEn: String,
    val sourceAr: String,
    val sourceEn: String,
    val quranRefs: List<AyahRef> = emptyList(),
    val bundledText: List<String> = emptyList(),
    val noteAr: String? = null,
    val noteEn: String? = null,
) {
    fun title(arabic: Boolean) = if (arabic) titleAr else titleEn
    fun source(arabic: Boolean) = if (arabic) sourceAr else sourceEn
}

object DuaCatalog {
    private const val MAFATIH_AR = "مفاتيح الجنان – الشيخ عباس القمي"
    private const val MAFATIH_EN = "Mafatih al-Jinan – Shaykh Abbas al-Qummi"
    private const val SAHIFA_AR = "الصحيفة السجادية – الإمام علي بن الحسين (ع)"
    private const val SAHIFA_EN = "Al-Sahifa al-Sajjadiyya – Imam Ali ibn al-Husayn (a)"
    private const val QURAN_AR = "القرآن الكريم"
    private const val QURAN_EN = "The Holy Quran"

    private fun r(s: Int, vararg a: Int) = a.map { AyahRef(s, it) }

    val entries: List<DuaEntry> = listOf(
        // Quranic supplications – text resolved from the verified Quran asset.
        DuaEntry("q_adam", DuaCategory.QURANIC, "دعاء آدم وحواء (ع)", "Supplication of Adam and Hawwa (a)", QURAN_AR, QURAN_EN, r(7, 23)),
        DuaEntry("q_dunya_akhira", DuaCategory.QURANIC, "حسنة الدنيا والآخرة", "Good in this world and the Hereafter", QURAN_AR, QURAN_EN, r(2, 201)),
        DuaEntry("q_baqara_end", DuaCategory.QURANIC, "خواتيم سورة البقرة", "Closing verses of al-Baqarah", QURAN_AR, QURAN_EN, r(2, 285, 286)),
        DuaEntry("q_steadfast", DuaCategory.QURANIC, "الثبات على الهداية", "Steadfastness upon guidance", QURAN_AR, QURAN_EN, r(3, 8, 9)),
        DuaEntry("q_ibrahim", DuaCategory.QURANIC, "دعاء إبراهيم (ع)", "Supplication of Ibrahim (a)", QURAN_AR, QURAN_EN, r(14, 40, 41)),
        DuaEntry("q_parents", DuaCategory.QURANIC, "الدعاء للوالدين", "For one's parents", QURAN_AR, QURAN_EN, r(17, 24)),
        DuaEntry("q_musa", DuaCategory.QURANIC, "دعاء موسى (ع)", "Supplication of Musa (a)", QURAN_AR, QURAN_EN, r(20, 25, 26, 27, 28)),
        DuaEntry("q_knowledge", DuaCategory.QURANIC, "طلب العلم", "For increase in knowledge", QURAN_AR, QURAN_EN, r(20, 114)),
        DuaEntry("q_yunus", DuaCategory.QURANIC, "دعاء يونس (ع)", "Supplication of Yunus (a)", QURAN_AR, QURAN_EN, r(21, 87)),
        DuaEntry("q_family", DuaCategory.QURANIC, "الذرية الصالحة", "For righteous family", QURAN_AR, QURAN_EN, r(25, 74)),
        DuaEntry("q_mercy", DuaCategory.QURANIC, "طلب المغفرة والرحمة", "For forgiveness and mercy", QURAN_AR, QURAN_EN, r(23, 118)),
        DuaEntry("q_gratitude", DuaCategory.QURANIC, "الشكر وصلاح الذرية", "Gratitude and righteous offspring", QURAN_AR, QURAN_EN, r(46, 15)),
        DuaEntry("q_believers", DuaCategory.QURANIC, "الدعاء للمؤمنين", "For the believers", QURAN_AR, QURAN_EN, r(59, 10)),

        // Well-known supplications – verified text must come from a content pack.
        DuaEntry("kumayl", DuaCategory.SUPPLICATIONS, "دعاء كميل", "Du'a Kumayl", MAFATIH_AR, MAFATIH_EN),
        DuaEntry("tawassul", DuaCategory.SUPPLICATIONS, "دعاء التوسل", "Du'a al-Tawassul", MAFATIH_AR, MAFATIH_EN),
        DuaEntry("nudba", DuaCategory.SUPPLICATIONS, "دعاء الندبة", "Du'a al-Nudba", MAFATIH_AR, MAFATIH_EN),
        DuaEntry("ahd", DuaCategory.SUPPLICATIONS, "دعاء العهد", "Du'a al-Ahd", MAFATIH_AR, MAFATIH_EN),
        DuaEntry("faraj", DuaCategory.SUPPLICATIONS, "دعاء الفرج", "Du'a al-Faraj", MAFATIH_AR, MAFATIH_EN),
        DuaEntry("sabah", DuaCategory.SUPPLICATIONS, "دعاء الصباح", "Du'a al-Sabah", MAFATIH_AR, MAFATIH_EN),
        DuaEntry("iftitah", DuaCategory.SUPPLICATIONS, "دعاء الافتتاح", "Du'a al-Iftitah", MAFATIH_AR, MAFATIH_EN),
        DuaEntry("abu_hamza", DuaCategory.SUPPLICATIONS, "دعاء أبي حمزة الثمالي", "Du'a Abu Hamza al-Thumali", MAFATIH_AR, MAFATIH_EN),
        DuaEntry("arafah", DuaCategory.SUPPLICATIONS, "دعاء الإمام الحسين (ع) يوم عرفة", "Du'a of Imam al-Husayn (a) on Arafah", MAFATIH_AR, MAFATIH_EN),
        DuaEntry("mashlool", DuaCategory.SUPPLICATIONS, "دعاء المشلول", "Du'a al-Mashlul", MAFATIH_AR, MAFATIH_EN),
        DuaEntry("jawshan_kabir", DuaCategory.SUPPLICATIONS, "دعاء الجوشن الكبير", "Du'a al-Jawshan al-Kabir", MAFATIH_AR, MAFATIH_EN),
        DuaEntry("makarim", DuaCategory.SAHIFA, "دعاء مكارم الأخلاق", "Du'a Makarim al-Akhlaq", SAHIFA_AR, SAHIFA_EN),

        // Ziyarat.
        DuaEntry("ashura", DuaCategory.ZIYARAT, "زيارة عاشوراء", "Ziyarat Ashura", MAFATIH_AR, MAFATIH_EN),
        DuaEntry("warith", DuaCategory.ZIYARAT, "زيارة وارث", "Ziyarat Warith", MAFATIH_AR, MAFATIH_EN),
        DuaEntry("aminullah", DuaCategory.ZIYARAT, "زيارة أمين الله", "Ziyarat Aminullah", MAFATIH_AR, MAFATIH_EN),
        DuaEntry("jamia_kabira", DuaCategory.ZIYARAT, "الزيارة الجامعة الكبيرة", "Ziyarat al-Jami'a al-Kabira", MAFATIH_AR, MAFATIH_EN),
        DuaEntry("arbaeen", DuaCategory.ZIYARAT, "زيارة الأربعين", "Ziyarat al-Arba'een", MAFATIH_AR, MAFATIH_EN),
        DuaEntry("al_yasin", DuaCategory.ZIYARAT, "زيارة آل يس", "Ziyarat Al-Yasin", MAFATIH_AR, MAFATIH_EN),
        DuaEntry("abbas", DuaCategory.ZIYARAT, "زيارة أبي الفضل العباس (ع)", "Ziyarat of Abu al-Fadl al-Abbas (a)", MAFATIH_AR, MAFATIH_EN),
        DuaEntry("fatima", DuaCategory.ZIYARAT, "زيارة السيدة فاطمة الزهراء (ع)", "Ziyarat of Lady Fatima al-Zahra (a)", MAFATIH_AR, MAFATIH_EN),

        // Munajat.
        DuaEntry("munajat_15", DuaCategory.MUNAJAT, "المناجاة الخمس عشرة", "The Fifteen Whispered Prayers", MAFATIH_AR, MAFATIH_EN),
        DuaEntry("munajat_shabaniyya", DuaCategory.MUNAJAT, "المناجاة الشعبانية", "Munajat Sha'baniyya", MAFATIH_AR, MAFATIH_EN),

        // Tasbihat – short formulas bundled verbatim.
        DuaEntry(
            "tasbih_zahra", DuaCategory.TASBIHAT,
            "تسبيح السيدة فاطمة الزهراء (ع)", "Tasbih of Lady Fatima al-Zahra (a)",
            MAFATIH_AR, MAFATIH_EN,
            bundledText = listOf("اللهُ أَكْبَرُ  (٣٤ مرة)", "الْحَمْدُ لِلَّهِ  (٣٣ مرة)", "سُبْحَانَ اللهِ  (٣٣ مرة)"),
            noteAr = "يمكنك أداؤه بالمسبحة الرقمية من خلال وضع «تسبيح الزهراء».",
            noteEn = "You can perform it with the digital Tasbih using the \"Tasbih al-Zahra\" mode.",
        ),
    )

    fun byId(id: String): DuaEntry? = entries.firstOrNull { it.id == id }

    const val NAHJ_SOURCE_AR = "نهج البلاغة – جمع الشريف الرضي"
    const val NAHJ_SOURCE_EN = "Nahj al-Balagha – compiled by al-Sharif al-Radi"
    const val SAHIFA_SOURCE_AR = SAHIFA_AR
    const val SAHIFA_SOURCE_EN = SAHIFA_EN

    /** Quranic supplications rotate as the verified "dua of the day". */
    fun duaOfTheDay(date: java.time.LocalDate): DuaEntry {
        val quranic = entries.filter { it.category == DuaCategory.QURANIC }
        return quranic[Math.floorMod(date.toEpochDay() * 7 + 3, quranic.size.toLong()).toInt()]
    }
}
