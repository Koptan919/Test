package com.noor.app.data.duas

import android.content.Context
import android.net.Uri
import com.noor.app.data.quran.QuranRepository
import com.noor.core.content.DuaCatalog
import com.noor.core.content.DuaCategory
import com.noor.core.content.DuaEntry
import com.noor.core.quran.Ayah
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/** Where a content pack came from. */
enum class PackOrigin { IMPORTED, SYNCED }

/** Verified texts supplied as a JSON content pack (see docs/CONTENT_PACK.md). */
data class ContentPack(
    val name: String,
    val source: String,
    val version: String?,
    val origin: PackOrigin,
    val items: Map<String, PackItem>,
    /** Entries the pack adds to the library (books such as Nahj al-Balagha), in pack order. */
    val newEntries: List<DuaEntry>,
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
    private val metaFile: File get() = File(context.filesDir, "content/pack.meta")
    private val _pack = MutableStateFlow<ContentPack?>(null)
    val pack: StateFlow<ContentPack?> = _pack.asStateFlow()
    @Volatile private var packLoaded = false

    /** Built-in catalogue plus entries added by the active pack. */
    fun entries(pack: ContentPack? = _pack.value): List<DuaEntry> = DuaCatalog.entries + pack?.newEntries.orEmpty()

    fun byId(id: String): DuaEntry? = entries().firstOrNull { it.id == id }

    suspend fun ensurePackLoaded() = withContext(Dispatchers.IO) {
        if (packLoaded) return@withContext
        _pack.value = runCatching {
            if (packFile.exists()) parse(packFile.readText(), readOrigin()) else null
        }.getOrNull()
        packLoaded = true
    }

    fun isAvailable(entry: DuaEntry, pack: ContentPack? = _pack.value): Boolean =
        entry.quranRefs.isNotEmpty() || entry.bundledText.isNotEmpty() || pack?.items?.containsKey(entry.id) == true

    suspend fun text(entry: DuaEntry, arabic: Boolean): DuaText {
        ensurePackLoaded()
        if (entry.quranRefs.isNotEmpty()) {
            val ayahs = entry.quranRefs.mapNotNull { quran.ayah(it) }
            return if (ayahs.size == entry.quranRefs.size) DuaText.Quranic(ayahs) else DuaText.Unavailable
        }
        if (entry.bundledText.isNotEmpty()) return DuaText.Lines(entry.bundledText, entry.source(arabic))
        val pack = _pack.value ?: return DuaText.Unavailable
        val item = pack.items[entry.id] ?: return DuaText.Unavailable
        return DuaText.Lines(item.lines, item.source ?: pack.source)
    }

    /** Validates and stores a pack chosen by the user. Returns the number of texts it provides. */
    suspend fun importPack(uri: Uri): Int = withContext(Dispatchers.IO) {
        val raw = context.contentResolver.openInputStream(uri)?.use { input ->
            val bytes = input.readBytes()
            if (bytes.size > MAX_PACK_BYTES) throw ContentPackException("File too large")
            bytes.toString(Charsets.UTF_8)
        } ?: throw ContentPackException("Cannot read file")
        install(raw, PackOrigin.IMPORTED)
    }

    /** Validates and stores a pack that has already passed checksum verification (content sync). */
    suspend fun installSynced(raw: String): Int = withContext(Dispatchers.IO) { install(raw, PackOrigin.SYNCED) }

    private fun install(raw: String, origin: PackOrigin): Int {
        val pack = parse(raw, origin)
        packFile.parentFile?.mkdirs()
        // Write atomically so a crash never leaves a half-written pack.
        val tmp = File(packFile.parentFile, "pack.json.tmp")
        tmp.writeText(raw)
        if (!tmp.renameTo(packFile)) {
            packFile.writeText(raw)
            tmp.delete()
        }
        metaFile.writeText(origin.name)
        _pack.value = pack
        packLoaded = true
        return pack.items.size
    }

    suspend fun removePack() = withContext(Dispatchers.IO) {
        packFile.delete()
        metaFile.delete()
        _pack.value = null
    }

    private fun readOrigin(): PackOrigin =
        runCatching { PackOrigin.valueOf(metaFile.readText().trim()) }.getOrDefault(PackOrigin.IMPORTED)

    companion object {
        const val FORMAT = "noor-content-pack"
        const val MAX_PACK_BYTES = 16 * 1024 * 1024
        private val ID_PATTERN = Regex("^[a-z0-9_]{2,64}$")

        /** Pure validation, shared by import and sync. */
        fun parse(raw: String, origin: PackOrigin = PackOrigin.IMPORTED): ContentPack {
            val json = runCatching { JSONObject(raw) }.getOrElse { throw ContentPackException("Invalid JSON") }
            if (json.optString("format") != FORMAT) throw ContentPackException("Not a Noor content pack")
            val source = json.optString("source").trim()
            if (source.isEmpty()) throw ContentPackException("The pack must name its source")
            val items = json.optJSONArray("items") ?: throw ContentPackException("Missing items")
            val known = DuaCatalog.entries.filter { it.quranRefs.isEmpty() && it.bundledText.isEmpty() }.map { it.id }.toSet()
            val builtIn = DuaCatalog.entries.map { it.id }.toSet()
            val map = LinkedHashMap<String, PackItem>()
            val added = ArrayList<DuaEntry>()
            for (i in 0 until items.length()) {
                val obj = items.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                val arr = obj.optJSONArray("text") ?: continue
                val lines = (0 until arr.length()).map { arr.optString(it).trim() }.filter { it.isNotEmpty() }
                if (lines.isEmpty() || !ID_PATTERN.matches(id) || map.containsKey(id)) continue
                val itemSource = obj.optString("source").trim().takeIf { it.isNotEmpty() }
                when {
                    id in known -> map[id] = PackItem(lines, itemSource)
                    id in builtIn -> continue // Quranic and bundled texts can never be overridden.
                    else -> {
                        val category = DuaCategory.fromKey(obj.optString("category")) ?: continue
                        if (!category.acceptsNewEntries) continue
                        val titleAr = obj.optString("titleAr").trim()
                        if (titleAr.isEmpty()) continue
                        val titleEn = obj.optString("titleEn").trim().ifEmpty { titleAr }
                        val src = itemSource ?: source
                        added += DuaEntry(id, category, titleAr, titleEn, src, src)
                        map[id] = PackItem(lines, itemSource)
                    }
                }
            }
            if (map.isEmpty()) throw ContentPackException("No valid entries")
            return ContentPack(
                name = json.optString("name").ifBlank { source },
                source = source,
                version = json.optString("version").takeIf { it.isNotBlank() },
                origin = origin,
                items = map,
                newEntries = added,
            )
        }
    }
}
