package com.noor.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.noor.app.data.remote.AppUpdater
import com.noor.app.data.remote.GitHubClient
import com.noor.app.data.remote.InstallState
import com.noor.app.data.remote.ReleaseAsset
import com.noor.app.data.remote.ReleaseInfo
import com.noor.app.data.remote.UpdateVerificationException
import com.noor.core.quran.QuranParser
import java.net.ServerSocket
import kotlin.concurrent.thread
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** The self-updater must refuse anything that is not a verified, newer build of Noor. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppUpdaterTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var server: ServerSocket
    private val files = mutableMapOf<String, ByteArray>()
    private lateinit var base: String

    @Before
    fun start() {
        // Minimal static HTTP server for release assets.
        server = ServerSocket(0, 50, java.net.InetAddress.getLoopbackAddress())
        thread(isDaemon = true) {
            while (!server.isClosed) {
                val socket = runCatching { server.accept() }.getOrNull() ?: break
                socket.use { sock ->
                    val reader = sock.getInputStream().bufferedReader()
                    val path = reader.readLine()?.split(" ")?.getOrNull(1).orEmpty()
                    while (reader.readLine()?.isNotEmpty() == true) Unit
                    val body = files[path]
                    val out = sock.getOutputStream()
                    if (body == null) {
                        out.write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
                    } else {
                        out.write("HTTP/1.1 200 OK\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n".toByteArray())
                        out.write(body)
                    }
                    out.flush()
                }
            }
        }
        base = "http://127.0.0.1:${server.localPort}"
    }

    @After fun stop() = server.close()

    private fun release(vararg assets: String) = ReleaseInfo(
        tag = "v9.9.9", name = "Noor", notes = "", pageUrl = "$base/page", publishedAt = null,
        prerelease = false, draft = false,
        assets = assets.map { ReleaseAsset(it, "$base/$it", 0) },
    )

    private val updater get() = AppUpdater(context, GitHubClient(base))

    @Test
    fun refusesReleaseWithoutChecksum() = runBlocking {
        files["/noor-v9.9.9.apk"] = byteArrayOf(1, 2, 3)
        val u = updater
        val e = runCatching { u.downloadAndVerify(release("noor-v9.9.9.apk")) }.exceptionOrNull()
        assertTrue(e is UpdateVerificationException)
        assertNull(u.pendingUpdate())
    }

    @Test
    fun refusesChecksumMismatch() = runBlocking {
        files["/noor-v9.9.9.apk"] = byteArrayOf(1, 2, 3)
        files["/noor-v9.9.9.apk.sha256"] = ("0".repeat(64) + "  noor-v9.9.9.apk").toByteArray()
        val u = updater
        val e = runCatching { u.downloadAndVerify(release("noor-v9.9.9.apk", "noor-v9.9.9.apk.sha256")) }.exceptionOrNull()
        assertEquals("checksum mismatch", e?.message)
        assertTrue(u.state.value is InstallState.Failed)
        assertNull("a rejected file must never be kept", u.pendingUpdate())
    }

    @Test
    fun failsClosedWhenPackageCannotBeVerified() = runBlocking {
        // A correctly checksummed file that is not a newer, identically signed Noor APK.
        // (Robolectric cannot run the platform APK parser, so this asserts the fail-closed
        // behaviour: any verification error means nothing is kept or installed.)
        val apk = File("build/outputs/apk/debug/app-debug.apk").takeIf { it.exists() }?.readBytes() ?: byteArrayOf(9, 9, 9)
        files["/noor-v9.9.9.apk"] = apk
        files["/noor-v9.9.9.apk.sha256"] = (QuranParser.sha256(apk) + "  noor-v9.9.9.apk").toByteArray()
        val u = updater
        val e = runCatching { u.downloadAndVerify(release("noor-v9.9.9.apk", "noor-v9.9.9.apk.sha256")) }.exceptionOrNull()
        assertTrue("download must be refused", e != null)
        assertTrue(u.state.value is InstallState.Failed)
        assertNull("an unverified APK must never be kept", u.pendingUpdate())
    }
}
