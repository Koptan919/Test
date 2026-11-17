package com.noor.app.data.remote

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.noor.app.BuildConfig
import com.noor.app.NoorApplication
import com.noor.app.data.duas.ContentPackException
import com.noor.app.data.duas.DuaRepository
import com.noor.app.data.duas.PackOrigin
import com.noor.app.data.prefs.SettingsRepository
import com.noor.core.quran.QuranParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

sealed interface SyncResult {
    data object Idle : SyncResult
    data object Running : SyncResult
    data class Updated(val version: String, val texts: Int) : SyncResult
    data object UpToDate : SyncResult
    data object NothingPublished : SyncResult
    /** A pack the user imported manually is active; sync never overwrites it. */
    data object ImportedPackActive : SyncResult
    data class Failed(val reason: String, val offline: Boolean = false) : SyncResult
}

/**
 * Downloads approved content packs published as releases tagged `content-*` in the
 * release repository. Each pack must be accompanied by a `.sha256` file; the checksum
 * and the pack's structure are validated before anything is stored.
 */
class ContentSync(
    private val client: GitHubClient,
    private val duas: DuaRepository,
    private val settings: SettingsRepository,
    private val connectivity: Connectivity,
    private val repo: String = BuildConfig.RELEASES_REPO,
) {
    private val _state = MutableStateFlow<SyncResult>(SyncResult.Idle)
    val state: StateFlow<SyncResult> = _state.asStateFlow()

    suspend fun sync(): SyncResult {
        _state.value = SyncResult.Running
        return runSync().also { _state.value = it }
    }

    private suspend fun runSync(): SyncResult {
        if (!connectivity.isOnline()) return SyncResult.Failed("offline", offline = true)
        duas.ensurePackLoaded()
        val current = duas.pack.value
        if (current != null && current.origin == PackOrigin.IMPORTED) return SyncResult.ImportedPackActive
        return try {
            val release = client.releases(repo)
                .filter { !it.draft && !it.prerelease && it.tag.startsWith(TAG_PREFIX) }
                .firstOrNull { it.asset(PACK_ASSET) != null && it.asset(CHECKSUM_ASSET) != null }
                ?: return SyncResult.NothingPublished
            val s = settings.current()
            if (release.tag == s.syncedContentVersion && current != null) {
                settings.setSyncedContent(release.tag, System.currentTimeMillis())
                return SyncResult.UpToDate
            }
            val expected = client.download(release.asset(CHECKSUM_ASSET)!!.downloadUrl, 4096)
                .toString(Charsets.UTF_8).trim().split(Regex("\\s+")).first().lowercase()
            if (!expected.matches(Regex("^[0-9a-f]{64}$"))) return SyncResult.Failed("bad checksum file")
            val bytes = client.download(release.asset(PACK_ASSET)!!.downloadUrl, DuaRepository.MAX_PACK_BYTES)
            if (QuranParser.sha256(bytes) != expected) return SyncResult.Failed("checksum mismatch")
            val texts = duas.installSynced(bytes.toString(Charsets.UTF_8))
            settings.setSyncedContent(release.tag, System.currentTimeMillis())
            SyncResult.Updated(release.tag, texts)
        } catch (e: ContentPackException) {
            SyncResult.Failed(e.message ?: "invalid pack")
        } catch (e: Exception) {
            SyncResult.Failed(e.message ?: e.javaClass.simpleName)
        }
    }

    companion object {
        const val TAG_PREFIX = "content-"
        const val PACK_ASSET = "noor-content-pack.json"
        const val CHECKSUM_ASSET = "noor-content-pack.json.sha256"
        private const val WORK_NAME = "content_sync"

        fun schedule(context: Context, enabled: Boolean) {
            val wm = WorkManager.getInstance(context)
            if (!enabled) {
                wm.cancelUniqueWork(WORK_NAME)
                return
            }
            val request = PeriodicWorkRequestBuilder<ContentSyncWorker>(7, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).setRequiresBatteryNotLow(true).build())
                .build()
            wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}

class ContentSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as NoorApplication).container
        if (!container.settings.current().autoContentSync) return Result.success()
        return when (val r = container.contentSync.sync()) {
            is SyncResult.Failed -> if (r.offline) Result.retry() else Result.success()
            else -> Result.success()
        }
    }
}
