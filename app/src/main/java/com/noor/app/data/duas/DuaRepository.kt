package com.noor.app.data.duas

import android.content.Context
import android.net.Uri
import com.noor.app.data.quran.QuranRepository
import com.noor.core.content.DuaCatalog
import com.noor.core.content.DuaEntry
import com.noor.core.quran.Ayah
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/** Verified texts supplied by the user as a JSON content pack (see docs/CONTENT_PACK.md). */
data class ContentPack(
    val name: String,
    val source: String,
    val items: Map<String, PackItem>,
)

data class PackItem(val lines: List<String>, val source: String?)

sealed interface DuaText {
    data class Quranic(val ayahs: List<Ayah>) : DuaText
    data class Lines(val lines: List<String>, val source: String) : DuaText
    data object Unavailable : DuaText
}

class ContentPackException(message: String) : Exception(message)

class DuaRepository(
    private val context: Context,
    private val quran: QuranRepository,
) {
    private val packFile: File get() = File(context.filesDir, "content/pack.json")
    private val _pack = MutableStateFlow<ContentPack?>(null)
    val pack: StateFlow<ContentPack?> = _pack.asStateFlow()
    @Volatile private var packLoaded = false

    val entries: List<DuaEntry> get() = DuaCatalog.entries

    suspend fun ensurePackLoaded() = withContext(Dispatchers.IO) {
        if (packLoaded) return@withContext
        _pack.value = runCatching { if (packFile.exists()) parse(packFile.readText()) else null }.getOrNull()
        packLoaded = true
    }

    suspend fun isAvailable(entry: DuaEntry): Boolean {
        ensurePackLoaded()
        return entry.quranRefs.isNotEmpty() || entry.bundledText.isNotEmpty() || _pack.value?.items?.containsKey(entry.id) == true
    }

    suspend fun text(entry: DuaEntry, arabic: Boolean): DuaText {
        ensurePackLoaded()
        if (entry.quranRefs.isNotEmpty()) {
            val ayahs = entry.quranRefs.mapNotNull { quran.ayah(it) }
            return if (ayahs.size == entry.quranRefs.size) DuaText.Quranic(ayahs) else DuaText.Unavailable
        }
        if (entry.bundledText.isNotEmpty()) return DuaText.Lines(entry.bundledText, entry.source(arabic))
        val item = _pack.value?.items?.get(entry.id) ?: return DuaText.Unavailable
        val pack = _pack.value!!
        return DuaText.Lines(item.lines, item.source ?: pack.source)
    }

    /** Validates and stores a content pack. Returns the number of catalogue entries it provides. */
    suspend fun importPack(uri: Uri): Int = withContext(Dispatchers.IO) {
        val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: throw ContentPackException("Cannot read file")
        if (raw.length > MAX_PACK_BYTES) throw ContentPackException("File too large")
        val pack = parse(raw)
        packFile.parentFile?.mkdirs()
        packFile.writeText(raw)
        _pack.value = pack
        packLoaded = true
        pack.items.size
    }

    suspend fun removePack() = withContext(Dispatchers.IO) {
        packFile.delete()
        _pack.value = null
    }

    private fun parse(raw: String): ContentPack {
        val json = runCatching { JSONObject(raw) }.getOrElse { throw ContentPackException("Invalid JSON") }
        if (json.optString("format") != FORMAT) throw ContentPackException("Not a Noor content pack")
        val source = json.optString("source").trim()
        if (source.isEmpty()) throw ContentPackException("The pack must name its source")
        val items = json.optJSONArray("items") ?: throw ContentPackException("Missing items")
        val known = DuaCatalog.entries.filter { it.quranRefs.isEmpty() && it.bundledText.isEmpty() }.map { it.id }.toSet()
        val map = LinkedHashMap<String, PackItem>()
        for (i in 0 until items.length()) {
            val obj = items.getJSONObject(i)
            val id = obj.optString("id")
            if (id !in known) continue
            val arr = obj.optJSONArray("text") ?: continue
            val lines = (0 until arr.length()).map { arr.getString(it).trim() }.filter { it.isNotEmpty() }
            if (lines.isEmpty()) continue
            map[id] = PackItem(lines, obj.optString("source").takeIf { it.isNotBlank() })
        }
        if (map.isEmpty()) throw ContentPackException("No matching entries")
        return ContentPack(json.optString("name").ifBlank { source }, source, map)
    }

    companion object {
        const val FORMAT = "noor-content-pack"
        private const val MAX_PACK_BYTES = 8 * 1024 * 1024
    }
}
