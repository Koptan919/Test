package com.noor.app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.noor.app.data.db.NoorDatabase
import com.noor.app.data.duas.ContentPackException
import com.noor.app.data.duas.DuaRepository
import com.noor.app.data.remote.GitHubClient
import com.noor.core.content.DuaCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    /** A v1 database (first release) must open in v2 with all user data intact. */
    @Test
    fun migrationFromV1KeepsUserData() {
        val v1 = JSONObject(File("schemas/com.noor.app.data.db.NoorDatabase/1.json").readText()).getJSONObject("database")
        val path = context.getDatabasePath("noor.db").apply { parentFile?.mkdirs(); delete() }
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            val entities = v1.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val e = entities.getJSONObject(i)
                db.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", e.getString("tableName")))
            }
            val setup = v1.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            db.execSQL("INSERT INTO favorites VALUES ('AYAH:1:1','AYAH','1:1','الفاتحة','Al-Fatiha 1:1',NULL,1)")
            db.execSQL("INSERT INTO reading_progress VALUES (18, 10, 110, 2)")
            db.execSQL("INSERT INTO tasbih_state VALUES (1,'ZAHRA',1,12,33,4,1000,3)")
            db.version = 1
        }
        val room = NoorDatabase.create(context)
        runBlocking {
            assertEquals(listOf("AYAH:1:1"), room.favoriteDao().observeKeys().first())
            assertEquals(10, room.readingDao().observeLatest().first()!!.ayah)
            assertEquals(12, room.tasbihDao().observe().first()!!.count)
            assertTrue(room.audioDao().observeAll().first().isEmpty())
        }
        room.close()
    }

    private fun pack(items: String, source: String = "Test edition") =
        """{"format":"noor-content-pack","version":"1","name":"T","source":"$source","items":[$items]}"""

    @Test
    fun contentPackValidation() {
        // Known entry accepted; new Nahj entry accepted with a title; Quranic and bundled entries can't be overridden.
        val p = DuaRepository.parse(
            pack(
                """{"id":"kumayl","text":["a","b"]},
                   {"id":"nahj_sermon_1","category":"nahj","titleAr":"الخطبة","text":["x"]},
                   {"id":"q_yunus","text":["fake"]},
                   {"id":"tasbih_zahra","text":["fake"]},
                   {"id":"no_title","category":"nahj","text":["x"]},
                   {"id":"bad","category":"quranic","titleAr":"x","text":["x"]}""",
            ),
        )
        assertEquals(setOf("kumayl", "nahj_sermon_1"), p.items.keys)
        assertEquals(DuaCategory.NAHJ, p.newEntries.single().category)
        assertTrue(runCatching { DuaRepository.parse(pack("""{"id":"kumayl","text":["a"]}""", source = "")) }.exceptionOrNull() is ContentPackException)
        assertTrue(runCatching { DuaRepository.parse("""{"format":"other"}""") }.exceptionOrNull() is ContentPackException)
    }

    @Test
    fun releaseParsing() {
        val r = GitHubClient.parseRelease(
            JSONObject(
                """{"tag_name":"v0.3.0","name":"Noor 0.3.0","body":"Changes","html_url":"https://github.com/x/y/releases/v0.3.0",
                   "prerelease":false,"draft":false,"published_at":"2026-01-01T00:00:00Z",
                   "assets":[{"name":"noor-0.3.0.apk","browser_download_url":"https://example/apk","size":10}]}""",
            ),
        )
        assertEquals("v0.3.0", r.tag)
        assertEquals("https://example/apk", r.apkAsset?.downloadUrl)
        assertNull(r.asset("missing"))
    }
}
