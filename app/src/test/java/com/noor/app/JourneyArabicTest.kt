package com.noor.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.noor.app.TestSupport.shot
import com.noor.app.TestSupport.str
import com.noor.app.data.prefs.ThemeMode
import com.noor.app.util.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Arabic UI: right-to-left layout, Arabic strings and Arabic-Indic digits. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class JourneyArabicTest {
    @get:Rule val rule = createEmptyComposeRule()
    private val folder = "ar-dark-phone"

    @Test
    fun arabicRtl() {
        TestSupport.prepare(AppLanguage.ARABIC, ThemeMode.DARK)
        val sc = TestSupport.launch()
        fun s(id: Int, vararg a: Any) = sc.str(id, *a)
        fun scrollTo(text: String) {
            val scrollables = rule.onAllNodes(hasScrollAction())
            for (i in 0 until scrollables.fetchSemanticsNodes().size) {
                if (runCatching { scrollables[i].performScrollToNode(hasText(text)) }.isSuccess) break
            }
            rule.waitForIdle()
        }
        fun clickText(text: String) {
            if (rule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()) {
                val scrollables = rule.onAllNodes(hasScrollAction())
                for (i in 0 until scrollables.fetchSemanticsNodes().size) {
                    runCatching { scrollables[i].performScrollToNode(hasText(text)) }
                    if (rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()) break
                }
            }
            val node = rule.onAllNodesWithText(text).onFirst()
            runCatching { node.performScrollTo() }
            node.performClick()
            rule.waitForIdle()
        }
        fun back() { sc.onActivity { it.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle() }

        var rtl = false
        sc.onActivity { rtl = it.resources.configuration.layoutDirection == android.view.View.LAYOUT_DIRECTION_RTL }
        assertEquals("Layout must be right-to-left in Arabic", true, rtl)
        assertEquals("مواقيت الصلاة", s(R.string.module_prayer))

        rule.onNodeWithText(s(R.string.greeting_salam)).assertIsDisplayed()
        clickText(s(R.string.prayer_choose_city))
        clickText("كربلاء المقدسة")
        rule.waitUntil(10_000) { rule.onAllNodesWithText(s(R.string.home_next_prayer)).fetchSemanticsNodes().isNotEmpty() }
        rule.shot(folder, "01_home")
        scrollTo(s(R.string.home_tools))
        rule.shot(folder, "02_home_lower")
        scrollTo(s(R.string.module_library))
        clickText(s(R.string.module_quran))
        rule.shot(folder, "03_quran_list")
        clickText("الكهف")
        rule.waitUntil(10_000) { rule.onAllNodesWithText("سورة الكهف").fetchSemanticsNodes().isNotEmpty() }
        rule.shot(folder, "04_quran_reader")
        back(); back()
        clickText(s(R.string.module_duas))
        rule.shot(folder, "05_duas")
        back()
        scrollTo(s(R.string.home_tools))
        clickText(s(R.string.module_calendar))
        rule.shot(folder, "06_calendar")
        back()
        clickText(s(R.string.module_tasbih))
        rule.waitUntil(10_000) { rule.onAllNodesWithText(s(R.string.tasbih_of_target, "٣٤")).fetchSemanticsNodes().isNotEmpty() }
        rule.shot(folder, "07_tasbih")
        back()
        clickText(s(R.string.module_qibla))
        rule.shot(folder, "08_qibla")
        back()
        clickText(s(R.string.nav_settings))
        rule.shot(folder, "09_settings")
        sc.close()
    }
}
