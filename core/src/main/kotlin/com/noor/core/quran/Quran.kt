package com.noor.core.quran

import java.security.MessageDigest

/** Metadata of a surah (from the Tanzil metadata set). */
data class SurahMeta(
    val number: Int,
    val nameArabic: String,
    val nameTransliterated: String,
    val nameEnglish: String,
    val isMeccan: Boolean,
    val ayahCount: Int,
)

data class Ayah(val surah: Int, val number: Int, val text: String)

data class AyahRef(val surah: Int, val ayah: Int) {
    override fun toString(): String = "$surah:$ayah"
}

object QuranMeta {
    const val SURAH_COUNT = 114
    const val AYAH_COUNT = 6236

    /** SHA-256 of assets/quran/quran-uthmani.txt (Tanzil Uthmani text). */
    const val TEXT_SHA256 = "0edb6b0f46f7b4216a8f332cd141bed075879df723a2b1f6dcc5e6a34df071a5"

    /** Exactly as in the verified text (1:1). */
    const val BISMILLAH = "\u0628\u0650\u0633\u0652\u0645\u0650\u0020\u0671\u0644\u0644\u0651\u064e\u0647\u0650\u0020\u0671\u0644\u0631\u0651\u064e\u062d\u0652\u0645\u064e\u0670\u0646\u0650\u0020\u0671\u0644\u0631\u0651\u064e\u062d\u0650\u064a\u0645\u0650"

    val surahs: List<SurahMeta> get() = SurahData.surahs
    val juzStarts: List<Pair<Int, Int>> get() = SurahData.juzStarts

    fun surah(number: Int): SurahMeta = SurahData.surahs[number - 1]

    /** Surahs whose text starts without a separate Bismillah header. */
    fun showsBismillahHeader(surah: Int): Boolean = surah != 1 && surah != 9

    /** Juz (1..30) containing the given ayah. */
    fun juzOf(surah: Int, ayah: Int): Int {
        var juz = 1
        SurahData.juzStarts.forEachIndexed { index, (s, a) ->
            if (surah > s || (surah == s && ayah >= a)) juz = index + 1
        }
        return juz
    }
}

class QuranIntegrityException(message: String) : Exception(message)

/**
 * Parses the Tanzil "sura|aya|text" format and verifies its integrity.
 * Throws [QuranIntegrityException] if the text is not byte-identical to the
 * verified edition or is structurally incomplete.
 */
object QuranParser {
    fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    fun parseVerified(bytes: ByteArray): Map<Int, List<Ayah>> {
        val hash = sha256(bytes)
        if (hash != QuranMeta.TEXT_SHA256) {
            throw QuranIntegrityException("Quran text checksum mismatch: $hash")
        }
        return parse(String(bytes, Charsets.UTF_8))
    }

    fun parse(content: String): Map<Int, List<Ayah>> {
        val bySurah = LinkedHashMap<Int, MutableList<Ayah>>()
        content.lineSequence().filter { it.isNotBlank() && !it.startsWith("#") }.forEach { line ->
            val parts = line.split('|', limit = 3)
            if (parts.size != 3) throw QuranIntegrityException("Malformed line: $line")
            val s = parts[0].toInt()
            val a = parts[1].toInt()
            bySurah.getOrPut(s) { mutableListOf() }.add(Ayah(s, a, parts[2]))
        }
        if (bySurah.size != QuranMeta.SURAH_COUNT) throw QuranIntegrityException("Expected 114 surahs")
        bySurah.forEach { (s, list) ->
            if (list.size != QuranMeta.surah(s).ayahCount) {
                throw QuranIntegrityException("Surah $s has ${list.size} ayahs")
            }
        }
        return bySurah
    }
}
