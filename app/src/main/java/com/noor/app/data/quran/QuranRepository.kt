package com.noor.app.data.quran

import android.content.Context
import com.noor.app.data.db.ReadingDao
import com.noor.app.data.db.ReadingProgressEntity
import com.noor.core.quran.Ayah
import com.noor.core.quran.AyahRef
import com.noor.core.quran.QuranMeta
import com.noor.core.quran.QuranParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface QuranText {
    data class Ready(val surahs: Map<Int, List<Ayah>>) : QuranText {
        fun ayahs(surah: Int): List<Ayah> = surahs[surah].orEmpty()
        fun ayah(ref: AyahRef): Ayah? = surahs[ref.surah]?.getOrNull(ref.ayah - 1)
    }

    data class Unavailable(val reason: String) : QuranText
}

/**
 * Loads the bundled Tanzil Uthmani text. The file is verified against a SHA-256
 * checksum; if verification fails, the app shows an "unavailable" state rather than
 * possibly altered text.
 */
class QuranRepository(
    private val context: Context,
    private val readingDao: ReadingDao,
) {
    private val mutex = Mutex()
    @Volatile private var cached: QuranText? = null

    suspend fun load(): QuranText = cached ?: withContext(Dispatchers.IO) {
        mutex.withLock {
            cached ?: runCatching {
                val bytes = context.assets.open(ASSET).use { it.readBytes() }
                QuranText.Ready(QuranParser.parseVerified(bytes)) as QuranText
            }.getOrElse { QuranText.Unavailable(it.message ?: it.javaClass.simpleName) }
                .also { cached = it }
        }
    }

    suspend fun ayah(ref: AyahRef): Ayah? = (load() as? QuranText.Ready)?.ayah(ref)

    val progress: Flow<List<ReadingProgressEntity>> = readingDao.observeAll()
    val latestProgress: Flow<ReadingProgressEntity?> = readingDao.observeLatest()

    suspend fun saveProgress(surah: Int, ayah: Int) {
        readingDao.upsert(ReadingProgressEntity(surah, ayah, QuranMeta.surah(surah).ayahCount))
    }

    companion object {
        const val ASSET = "quran/quran-uthmani.txt"
    }
}
