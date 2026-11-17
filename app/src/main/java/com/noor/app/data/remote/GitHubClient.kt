package com.noor.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseAsset(val name: String, val downloadUrl: String, val size: Long)

data class ReleaseInfo(
    val tag: String,
    val name: String,
    val notes: String,
    val pageUrl: String,
    val publishedAt: String?,
    val prerelease: Boolean,
    val draft: Boolean,
    val assets: List<ReleaseAsset>,
) {
    val apkAsset: ReleaseAsset? get() = assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
    fun asset(name: String): ReleaseAsset? = assets.firstOrNull { it.name == name }
}

class NetworkException(message: String, val httpCode: Int? = null) : IOException(message)

/**
 * Minimal read-only client for the public GitHub REST API (no authentication, no
 * personal data sent). Uses HttpURLConnection to avoid extra dependencies.
 */
class GitHubClient(private val apiBase: String = "https://api.github.com") {

    /** Latest stable release, or null if the repository has none (404). */
    suspend fun latestRelease(repo: String): ReleaseInfo? {
        val body = get("$apiBase/repos/$repo/releases/latest", allow404 = true) ?: return null
        return parseRelease(JSONObject(body.toString(Charsets.UTF_8)))
    }

    suspend fun releases(repo: String): List<ReleaseInfo> {
        val body = get("$apiBase/repos/$repo/releases?per_page=30", allow404 = true) ?: return emptyList()
        val arr = JSONArray(body.toString(Charsets.UTF_8))
        return (0 until arr.length()).map { parseRelease(arr.getJSONObject(it)) }
    }

    suspend fun download(url: String, maxBytes: Int): ByteArray =
        get(url, allow404 = false, maxBytes = maxBytes, accept = "application/octet-stream")!!

    private suspend fun get(
        url: String,
        allow404: Boolean,
        maxBytes: Int = 2 * 1024 * 1024,
        accept: String = "application/vnd.github+json",
    ): ByteArray? = withContext(Dispatchers.IO) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("Accept", accept)
            setRequestProperty("User-Agent", "Noor-Android")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        }
        try {
            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_NOT_FOUND && allow404) return@withContext null
            if (code !in 200..299) throw NetworkException("HTTP $code", code)
            conn.inputStream.use { input ->
                val out = ByteArrayOutputStream()
                val buf = ByteArray(16 * 1024)
                var total = 0
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    total += n
                    if (total > maxBytes) throw NetworkException("Response too large")
                    out.write(buf, 0, n)
                }
                out.toByteArray()
            }
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        private const val TIMEOUT_MS = 15_000

        fun parseRelease(o: JSONObject): ReleaseInfo {
            val assetsJson = o.optJSONArray("assets") ?: JSONArray()
            val assets = (0 until assetsJson.length()).map {
                val a = assetsJson.getJSONObject(it)
                ReleaseAsset(a.optString("name"), a.optString("browser_download_url"), a.optLong("size"))
            }
            return ReleaseInfo(
                tag = o.optString("tag_name"),
                name = o.optString("name").ifBlank { o.optString("tag_name") },
                notes = o.optString("body"),
                pageUrl = o.optString("html_url"),
                publishedAt = o.optString("published_at").takeIf { it.isNotBlank() && it != "null" },
                prerelease = o.optBoolean("prerelease"),
                draft = o.optBoolean("draft"),
                assets = assets,
            )
        }
    }
}
