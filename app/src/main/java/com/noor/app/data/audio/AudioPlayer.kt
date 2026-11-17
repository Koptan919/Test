package com.noor.app.data.audio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import com.noor.app.data.db.AudioDao
import com.noor.app.data.db.AudioTrackEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PlayerState(
    val current: AudioTrackEntity? = null,
    val playing: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val error: Boolean = false,
)

/**
 * Plays audio files the user added from their own device. Nothing is streamed or
 * bundled: Noor does not distribute recordings it has no licence for.
 */
class AudioLibrary(private val context: Context, private val dao: AudioDao) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var player: MediaPlayer? = null
    private var ticker: Job? = null
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()
    val tracks: Flow<List<AudioTrackEntity>> = dao.observeAll()

    /** Keeps read access to the chosen files across restarts and stores them in the library. */
    suspend fun add(uris: List<Uri>) = withContext(Dispatchers.IO) {
        uris.forEach { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            dao.upsert(AudioTrackEntity(uri.toString(), displayName(uri), duration(uri)))
        }
    }

    suspend fun remove(track: AudioTrackEntity) {
        if (_state.value.current?.uri == track.uri) stop()
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.releasePersistableUriPermission(Uri.parse(track.uri), Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            dao.delete(track.uri)
        }
    }

    fun play(track: AudioTrackEntity) {
        if (_state.value.current?.uri == track.uri && player != null) {
            toggle()
            return
        }
        release()
        val mp = MediaPlayer()
        player = mp
        _state.value = PlayerState(current = track)
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build(),
            )
            mp.setDataSource(context, Uri.parse(track.uri))
            mp.setOnPreparedListener {
                it.start()
                _state.value = _state.value.copy(playing = true, durationMs = it.duration.toLong())
                startTicker()
            }
            mp.setOnCompletionListener {
                _state.value = _state.value.copy(playing = false, positionMs = _state.value.durationMs)
                ticker?.cancel()
            }
            mp.setOnErrorListener { _, _, _ ->
                _state.value = _state.value.copy(playing = false, error = true)
                true
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            _state.value = PlayerState(current = track, error = true)
            release()
        }
    }

    fun toggle() {
        val mp = player ?: return
        if (mp.isPlaying) {
            mp.pause()
            ticker?.cancel()
            _state.value = _state.value.copy(playing = false)
        } else {
            mp.start()
            _state.value = _state.value.copy(playing = true)
            startTicker()
        }
    }

    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs.toInt())
        _state.value = _state.value.copy(positionMs = positionMs)
    }

    fun stop() {
        release()
        _state.value = PlayerState()
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                player?.let { _state.value = _state.value.copy(positionMs = it.currentPosition.toLong()) }
                delay(500)
            }
        }
    }

    private fun release() {
        ticker?.cancel()
        player?.release()
        player = null
    }

    private fun displayName(uri: Uri): String =
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()?.substringBeforeLast('.') ?: uri.lastPathSegment ?: "Audio"

    private fun duration(uri: Uri): Long = runCatching {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(context, uri)
            r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } finally {
            r.release()
        }
    }.getOrDefault(0L)
}
