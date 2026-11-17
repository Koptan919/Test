package com.noor.core

import com.noor.core.content.DailyVerses
import com.noor.core.content.DuaCatalog
import com.noor.core.quran.QuranMeta
import com.noor.core.quran.QuranParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class QuranTest {
    private val bytes = File("../app/src/main/assets/quran/quran-uthmani.txt").readBytes()

    @Test
    fun bundledTextIsVerifiedAndComplete() {
        val quran = QuranParser.parseVerified(bytes)
        assertEquals(114, quran.size)
        assertEquals(6236, quran.values.sumOf { it.size })
        assertEquals(QuranMeta.BISMILLAH, quran.getValue(1).first().text)
        assertEquals("الٓمٓ", quran.getValue(2).first().text)
    }

    @Test(expected = com.noor.core.quran.QuranIntegrityException::class)
    fun tamperedTextIsRejected() {
        val tampered = bytes.copyOf().also { it[it.size - 2] = 'x'.code.toByte() }
        QuranParser.parseVerified(tampered)
    }

    @Test
    fun allContentReferencesExist() {
        val refs = DuaCatalog.entries.flatMap { it.quranRefs } + DailyVerses.refs
        refs.forEach { ref ->
            assertTrue("$ref", ref.surah in 1..114 && ref.ayah in 1..QuranMeta.surah(ref.surah).ayahCount)
        }
        assertEquals(DuaCatalog.entries.size, DuaCatalog.entries.map { it.id }.toSet().size)
    }

    @Test
    fun juzLookup() {
        assertEquals(1, QuranMeta.juzOf(1, 1))
        assertEquals(2, QuranMeta.juzOf(2, 142))
        assertEquals(30, QuranMeta.juzOf(114, 6))
        assertEquals(30, QuranMeta.juzStarts.size)
    }
}
