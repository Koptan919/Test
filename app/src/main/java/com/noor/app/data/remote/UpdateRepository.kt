package com.noor.app.data.remote

import com.noor.app.BuildConfig
import com.noor.app.data.prefs.SettingsRepository
import com.noor.core.update.Version
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface UpdateCheck {
    data object Idle : UpdateCheck
    data object Checking : UpdateCheck
    data class Available(val release: ReleaseInfo) : UpdateCheck
    data object UpToDate : UpdateCheck
    data object NoReleases : UpdateCheck
    data class Failed(val offline: Boolean) : UpdateCheck
}

/**
 * Checks the public release repository for a newer stable APK. It never downloads or
 * installs anything itself: the user is sent to the official release page.
 */
class UpdateRepository(
    private val client: GitHubClient,
    private val settings: SettingsRepository,
    private val connectivity: Connectivity,
    private val repo: String = BuildConfig.RELEASES_REPO,
    private val installedVersion: String = BuildConfig.VERSION_NAME,
) {
    private val _state = MutableStateFlow<UpdateCheck>(UpdateCheck.Idle)
    val state: StateFlow<UpdateCheck> = _state.asStateFlow()

    /** Update offered automatically at startup (null when none, dismissed or disabled). */
    private val _prompt = MutableStateFlow<ReleaseInfo?>(null)
    val prompt: StateFlow<ReleaseInfo?> = _prompt.asStateFlow()

    suspend fun checkInBackgroundIfDue(now: Long = System.currentTimeMillis()) {
        val s = settings.current()
        if (!s.autoUpdateCheck || now - s.lastUpdateCheck < CHECK_INTERVAL_MS || !connectivity.isOnline()) return
        val result = check()
        if (result is UpdateCheck.Available && result.release.tag != s.dismissedUpdate) _prompt.value = result.release
    }

    suspend fun check(): UpdateCheck {
        if (!connectivity.isOnline()) return UpdateCheck.Failed(offline = true).also { _state.value = it }
        _state.value = UpdateCheck.Checking
        val result = runCatching { latestAppRelease(client.releases(repo)) }.fold(
            onSuccess = { release ->
                when {
                    release == null -> UpdateCheck.NoReleases
                    Version.isNewer(release.tag, installedVersion) -> UpdateCheck.Available(release)
                    else -> UpdateCheck.UpToDate
                }
            },
            onFailure = { UpdateCheck.Failed(offline = false) },
        )
        settings.markUpdateChecked(System.currentTimeMillis())
        _state.value = result
        return result
    }

    fun showPrompt(release: ReleaseInfo) { _prompt.value = release }

    suspend fun dismiss(release: ReleaseInfo) {
        settings.dismissUpdate(release.tag)
        _prompt.value = null
    }

    fun closePrompt() { _prompt.value = null }

    companion object {
        const val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L
        private val APP_TAG = Regex("""^v\d+(\.\d+){0,2}$""")

        /**
         * The newest published, stable *app* release (tag `vX.Y.Z` with an APK). Content packs
         * (`content-*` tags) share the repository and must never hide an app update.
         */
        fun latestAppRelease(releases: List<ReleaseInfo>): ReleaseInfo? =
            releases.filter { !it.draft && !it.prerelease && APP_TAG.matches(it.tag) && it.apkAsset != null }
                .maxWithOrNull(compareBy { Version.parse(it.tag) })
    }
}
