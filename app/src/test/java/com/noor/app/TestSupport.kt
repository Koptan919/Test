package com.noor.app

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.noor.app.data.prefs.SettingsRepository
import com.noor.app.data.prefs.ThemeMode
import com.noor.app.util.AppLanguage
import com.noor.app.util.LocaleManager
import kotlinx.coroutines.runBlocking
import java.io.File

/** Shared setup for the screen-level journeys. */
object TestSupport {
    val outDir = File(System.getProperty("user.dir"), "build/outputs/screenshots")

    fun prepare(language: AppLanguage, theme: ThemeMode) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        LocaleManager.set(context, language)
        runBlocking {
            val settings = SettingsRepository(context)
            settings.setThemeMode(theme)
            // Deterministic tests: no infinite animations, no network calls at startup.
            settings.setReduceMotion(true)
            settings.setAutoUpdateCheck(false)
            settings.setAutoContentSync(false)
        }
    }

    fun launch(): ActivityScenario<MainActivity> = ActivityScenario.launch(MainActivity::class.java)

    fun ActivityScenario<MainActivity>.str(id: Int, vararg args: Any): String {
        var out = ""
        onActivity { a: Activity -> out = a.getString(id, *args) }
        return out
    }

    /** Draws every window (activity, dialogs, sheets) in z-order into one bitmap. */
    fun ComposeTestRule.shot(folder: String, name: String) {
        waitForIdle()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        val wmg = Class.forName("android.view.WindowManagerGlobal")
        val global = wmg.getMethod("getInstance").invoke(null)
        @Suppress("UNCHECKED_CAST")
        val names = (wmg.getMethod("getViewRootNames").invoke(global) as Array<String>)
        val roots = names.map { wmg.getMethod("getRootView", String::class.java).invoke(global, it) as android.view.View }
            .filter { it.isShown && it.width > 0 }
        val base = roots.first()
        val bitmap = Bitmap.createBitmap(base.width, base.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        roots.forEach { v ->
            val loc = IntArray(2)
            v.getLocationOnScreen(loc)
            canvas.save()
            canvas.translate(loc[0].toFloat(), loc[1].toFloat())
            v.draw(canvas)
            canvas.restore()
        }
        val dir = File(outDir, folder).apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
