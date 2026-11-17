package com.noor.app.data.remote

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.noor.app.NoorApplication
import com.noor.app.notifications.PrayerNotifier
import com.noor.core.quran.QuranParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

sealed interface InstallState {
    data object Idle : InstallState
    data class Downloading(val tag: String) : InstallState
    /** Downloaded and fully verified; ready to hand to the system installer. */
    data class Ready(val release: ReleaseInfo, val file: File) : InstallState
    data object Installing : InstallState
    data class Failed(val reason: String) : InstallState
}

class UpdateVerificationException(message: String) : Exception(message)

/**
 * Downloads, verifies and installs app updates from the official release repository.
 *
 * An APK is installed only if ALL of these hold:
 *  - its SHA-256 matches the `.apk.sha256` file published with the release;
 *  - its package name is this app's package name;
 *  - its version code is higher than the installed one;
 *  - it is signed with exactly the same certificate as the installed app.
 *
 * Installation goes through the system [PackageInstaller]. Android asks the user once to
 * allow Noor to install updates; on Android 12+ later updates can install without a prompt
 * because Noor becomes the installer of record. App data is always preserved.
 */
class AppUpdater(
    private val context: Context,
    private val client: GitHubClient,
) {
    private val _state = MutableStateFlow<InstallState>(InstallState.Idle)
    val state: StateFlow<InstallState> = _state.asStateFlow()
    private val dir: File get() = File(context.noBackupFilesDir, "updates").apply { mkdirs() }

    /** Whether Android currently allows Noor to install packages. */
    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    suspend fun downloadAndVerify(release: ReleaseInfo): File = withContext(Dispatchers.IO) {
        val apk = release.apkAsset ?: throw UpdateVerificationException("no APK in release")
        val checksumAsset = release.asset("${apk.name}.sha256") ?: throw UpdateVerificationException("no checksum published")
        _state.value = InstallState.Downloading(release.tag)
        try {
            val expected = client.download(checksumAsset.downloadUrl, 4096).toString(Charsets.UTF_8)
                .trim().split(Regex("\\s+")).first().lowercase()
            if (!expected.matches(Regex("^[0-9a-f]{64}$"))) throw UpdateVerificationException("bad checksum file")
            dir.listFiles()?.forEach { it.delete() } // keep only one pending update
            val target = File(dir, "noor-${release.tag}.apk")
            val bytes = client.download(apk.downloadUrl, MAX_APK_BYTES)
            if (QuranParser.sha256(bytes) != expected) throw UpdateVerificationException("checksum mismatch")
            target.writeBytes(bytes)
            try {
                verifyPackage(target)
            } catch (e: Throwable) {
                target.delete() // never keep a file that failed verification
                throw e
            }
            InstallState.Ready(release, target).also { _state.value = it }
            target
        } catch (e: Throwable) {
            // Fail closed: whatever went wrong, nothing unverified is kept or installed.
            _state.value = InstallState.Failed(e.message ?: e.javaClass.simpleName)
            throw e
        }
    }

    /** Checks package name, version and signing certificate against the installed app. */
    fun verifyPackage(apk: File) {
        val installed = installedInfo()
        val archive = archiveInfo(apk.absolutePath) ?: throw UpdateVerificationException("not a valid APK")
        if (archive.packageName != context.packageName) throw UpdateVerificationException("different package")
        if (PackageInfoCompat.getLongVersionCode(archive) <= PackageInfoCompat.getLongVersionCode(installed)) {
            throw UpdateVerificationException("not a newer version")
        }
        val installedCerts = certificates(installed)
        val newCerts = certificates(archive)
        if (installedCerts.isEmpty() || installedCerts != newCerts) throw UpdateVerificationException("signature does not match")
    }

    /** Hands a verified APK to the system installer. Never call with an unverified file. */
    fun install(file: File) {
        verifyPackage(file)
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("noor.apk", 0, file.length()).use { out ->
                file.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            val intent = Intent(context, UpdateInstallReceiver::class.java).setPackage(context.packageName)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
            val pending = PendingIntent.getBroadcast(context, sessionId, intent, flags)
            session.commit(pending.intentSender)
        }
        _state.value = InstallState.Installing
    }

    fun pendingUpdate(): File? = dir.listFiles()?.firstOrNull { it.extension == "apk" }

    fun clear() {
        dir.listFiles()?.forEach { it.delete() }
        _state.value = InstallState.Idle
    }

    internal fun reportFailure(reason: String) { _state.value = InstallState.Failed(reason) }

    @Suppress("DEPRECATION")
    private fun installedInfo(): PackageInfo {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        return context.packageManager.getPackageInfo(context.packageName, flags)
    }

    @Suppress("DEPRECATION")
    private fun archiveInfo(path: String): PackageInfo? {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        return context.packageManager.getPackageArchiveInfo(path, flags)
    }

    @Suppress("DEPRECATION")
    private fun certificates(info: PackageInfo): Set<String> {
        val sigs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val s = info.signingInfo ?: return emptySet()
            if (s.hasMultipleSigners()) s.apkContentsSigners else s.signingCertificateHistory
        } else {
            info.signatures
        } ?: return emptySet()
        val md = MessageDigest.getInstance("SHA-256")
        return sigs.map { sig -> md.digest(sig.toByteArray()).joinToString("") { "%02x".format(it) } }.toSet()
    }

    companion object {
        private const val MAX_APK_BYTES = 120 * 1024 * 1024
        private const val WORK_NAME = "auto_update"

        fun schedule(context: Context, enabled: Boolean) {
            val wm = WorkManager.getInstance(context)
            if (!enabled) {
                wm.cancelUniqueWork(WORK_NAME)
                return
            }
            val request = PeriodicWorkRequestBuilder<AutoUpdateWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).setRequiresBatteryNotLow(true).build())
                .build()
            wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}

/** Receives the system installer's result for an update session. */
class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? NoorApplication ?: return
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // Android needs the user's confirmation (first update, or Android < 12).
                @Suppress("DEPRECATION")
                val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT) ?: return
                confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(confirm) }
                    .onFailure { PrayerNotifier.showUpdateReady(context) }
            }
            PackageInstaller.STATUS_SUCCESS -> app.container.updater.clear()
            else -> {
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "install failed"
                app.container.updater.reportFailure(message)
            }
        }
    }
}

/** Daily on Wi-Fi: check, download, verify and (if allowed) install the latest stable release. */
class AutoUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as NoorApplication).container
        val settings = container.settings.current()
        if (!settings.autoUpdateCheck) return Result.success()
        val check = container.updates.check()
        if (check !is UpdateCheck.Available) return if (check is UpdateCheck.Failed && check.offline) Result.retry() else Result.success()
        val file = runCatching { container.updater.downloadAndVerify(check.release) }.getOrElse { return Result.success() }
        if (settings.autoInstallUpdates && container.updater.canInstall()) {
            runCatching { container.updater.install(file) }.onFailure { PrayerNotifier.showUpdateReady(applicationContext) }
        } else {
            PrayerNotifier.showUpdateReady(applicationContext)
        }
        return Result.success()
    }
}
