package com.noor.app.ui.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noor.app.AppContainer
import com.noor.app.data.db.FavoriteEntity
import com.noor.app.data.db.FavoriteType
import com.noor.app.data.db.ReadingProgressEntity
import com.noor.app.data.quran.QuranText
import com.noor.core.quran.Ayah
import com.noor.core.quran.QuranMeta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SurahListViewModel(container: AppContainer) : ViewModel() {
    val progress: StateFlow<Map<Int, ReadingProgressEntity>> = container.quran.progress
        .map { list -> list.associateBy { it.surah } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val latest: StateFlow<ReadingProgressEntity?> = container.quran.latestProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

sealed interface ReaderState {
    data object Loading : ReaderState
    data class Ready(val ayahs: List<Ayah>) : ReaderState
    data class Unavailable(val reason: String) : ReaderState
}

class QuranReaderViewModel(private val container: AppContainer, val surah: Int) : ViewModel() {
    private val _state = MutableStateFlow<ReaderState>(ReaderState.Loading)
    val state: StateFlow<ReaderState> = _state.asStateFlow()

    val settings = container.settings.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val favoriteKeys: StateFlow<Set<String>> = container.database.favoriteDao().observeKeys()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    init {
        viewModelScope.launch {
            _state.value = when (val text = container.quran.load()) {
                is QuranText.Ready -> ReaderState.Ready(text.ayahs(surah))
                is QuranText.Unavailable -> ReaderState.Unavailable(text.reason)
            }
        }
    }

    fun saveProgress(ayah: Int) {
        viewModelScope.launch {
            container.quran.saveProgress(surah, ayah)
            container.settings.setLastRead(surah, ayah)
        }
    }

    fun setFontSize(size: Float) {
        viewModelScope.launch { container.settings.setQuranFontSize(size) }
    }

    fun setLineSpacing(value: Float) {
        viewModelScope.launch { container.settings.setLineSpacing(value) }
    }

    fun setKeepScreenOn(value: Boolean) {
        viewModelScope.launch { container.settings.setKeepScreenOn(value) }
    }

    fun markOpened() {
        viewModelScope.launch { container.settings.addRecent("surah:$surah") }
    }

    fun toggleSurahFavorite() {
        val meta = QuranMeta.surah(surah)
        toggle(
            FavoriteEntity(
                key = FavoriteEntity.keyOf(FavoriteType.SURAH, surah.toString()),
                type = FavoriteType.SURAH.name,
                refId = surah.toString(),
                titleAr = "سورة ${meta.nameArabic}",
                titleEn = "${meta.nameTransliterated} (${meta.nameEnglish})",
                preview = null,
            ),
        )
    }

    fun toggleAyahFavorite(ayah: Ayah) {
        val meta = QuranMeta.surah(ayah.surah)
        toggle(
            FavoriteEntity(
                key = FavoriteEntity.keyOf(FavoriteType.AYAH, "${ayah.surah}:${ayah.number}"),
                type = FavoriteType.AYAH.name,
                refId = "${ayah.surah}:${ayah.number}",
                titleAr = "سورة ${meta.nameArabic} – الآية ${ayah.number}",
                titleEn = "${meta.nameTransliterated} ${ayah.surah}:${ayah.number}",
                preview = ayah.text,
            ),
        )
    }

    private fun toggle(entity: FavoriteEntity) {
        viewModelScope.launch {
            val dao = container.database.favoriteDao()
            if (dao.count(entity.key) > 0) dao.delete(entity.key) else dao.upsert(entity)
        }
    }
}
