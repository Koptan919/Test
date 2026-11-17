package com.noor.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.hasScrollAction
import com.noor.app.TestSupport.shot
import com.noor.app.TestSupport.str
import com.noor.app.data.prefs.ThemeMode
import com.noor.app.util.AppLanguage
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * End-to-end journey through every screen (English, dark, typical phone), asserting that
 * each button leads somewhere real, and saving screenshots for review.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class JourneyEnglishDarkTest {
    @get:Rule val rule = createEmptyComposeRule()
    private val folder = "en-dark-phone"

    @Test
    fun everyScreenWorks() {
        TestSupport.prepare(AppLanguage.ENGLISH, ThemeMode.DARK)
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
        fun visible(text: String) = rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        fun backUntil(text: String) {
            repeat(4) { if (visible(text)) return; back() }
            rule.onAllNodesWithText(text).onFirst().assertExists()
        }

        // Home without a city: no prayer times are invented.
        rule.onNodeWithText(s(R.string.prayer_no_location_title)).assertIsDisplayed()
        rule.shot(folder, "01_home_no_city")

        // Choose a city.
        clickText(s(R.string.prayer_choose_city))
        rule.onNodeWithText(s(R.string.city_search_hint)).assertIsDisplayed()
        rule.shot(folder, "02_city_picker")
        clickText("Najaf")
        rule.waitUntil(10_000) { rule.onAllNodesWithText(s(R.string.home_next_prayer)).fetchSemanticsNodes().isNotEmpty() }
        rule.shot(folder, "03_home")
        scrollTo(s(R.string.home_tools))
        rule.shot(folder, "04_home_lower")

        // Quran: list, reader, bookmark.
        scrollTo(s(R.string.module_library))
        clickText(s(R.string.module_quran))
        rule.onNodeWithText("Al-Faatiha").assertIsDisplayed()
        rule.shot(folder, "05_quran_list")
        clickText("Al-Faatiha")
        rule.waitUntil(10_000) { rule.onAllNodesWithContentDescription(s(R.string.action_bookmark)).fetchSemanticsNodes().isNotEmpty() }
        rule.shot(folder, "06_quran_reader")
        rule.onAllNodesWithContentDescription(s(R.string.action_bookmark)).onFirst().performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithContentDescription(s(R.string.action_remove_bookmark)).fetchSemanticsNodes().isNotEmpty() }
        // Focus mode hides all chrome; tapping the text restores it.
        rule.onNode(hasContentDescription(s(R.string.reading_focus))).performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithContentDescription(s(R.string.reading_focus)).fetchSemanticsNodes().isEmpty() }
        rule.shot(folder, "07_focus_mode")
        rule.onNode(hasScrollAction()).performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithContentDescription(s(R.string.reading_settings)).fetchSemanticsNodes().isNotEmpty() }
        rule.onNode(hasContentDescription(s(R.string.reading_settings))).performClick()
        rule.onNodeWithText(s(R.string.reading_line_spacing)).assertIsDisplayed()
        rule.shot(folder, "08_reading_sheet")
        backUntil(s(R.string.nav_home))

        // Duas: catalogue and the honest unavailable state.
        clickText(s(R.string.module_duas))
        rule.shot(folder, "09_duas")
        rule.onNode(hasSetTextAction()).performTextInput("Kumayl")
        rule.onNodeWithText("Du'a Kumayl").assertIsDisplayed()
        clickText("Du'a Kumayl")
        rule.onNodeWithText(s(R.string.duas_unavailable_title)).assertIsDisplayed()
        rule.shot(folder, "10_dua_unavailable")
        back()
        rule.onNode(hasSetTextAction()).performTextClearance()
        rule.onNode(hasSetTextAction()).performTextInput("Yunus")
        clickText("Supplication of Yunus (a)")
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Al-Anbiyaa 21:87").fetchSemanticsNodes().isNotEmpty() }
        rule.shot(folder, "11_dua_quranic")
        backUntil(s(R.string.nav_home))

        // Prayer times screen.
        clickText(s(R.string.module_prayer))
        rule.onNodeWithText(s(R.string.prayer_midnight)).assertExists()
        rule.shot(folder, "12_prayer_times")
        back()

        // Library and its shelves.
        scrollTo(s(R.string.module_library))
        clickText(s(R.string.module_library))
        rule.shot(folder, "13_library")
        clickText(s(R.string.category_nahj))
        rule.onNodeWithText(s(R.string.library_book_empty_title, s(R.string.category_nahj))).assertIsDisplayed()
        rule.shot(folder, "14_nahj_empty")
        back()
        scrollTo(s(R.string.marja_title))
        clickText(s(R.string.marja_title))
        rule.onNodeWithText("www.sistani.org").assertIsDisplayed()
        rule.shot(folder, "15_marja")
        backUntil(s(R.string.nav_home))

        // Tools.
        scrollTo(s(R.string.home_tools))
        clickText(s(R.string.module_qibla))
        rule.onNodeWithText(s(R.string.qibla_bearing, "202"), substring = false).assertExists()
        rule.shot(folder, "16_qibla")
        back()
        clickText(s(R.string.module_tasbih))
        rule.waitUntil(10_000) { rule.onAllNodesWithText(s(R.string.tasbih_of_target, "34")).fetchSemanticsNodes().isNotEmpty() }
        val counter = rule.onNode(hasText(s(R.string.tasbih_of_target, "34")))
        repeat(5) { counter.performClick() }
        org.junit.Assert.assertEquals("counter and total both show 5 taps", 2, rule.onAllNodesWithText("5").fetchSemanticsNodes().size)
        rule.shot(folder, "17_tasbih")
        back()
        clickText(s(R.string.module_calendar))
        rule.shot(folder, "18_calendar")
        back()
        clickText(s(R.string.module_audio))
        rule.onNodeWithText(s(R.string.audio_empty_title)).assertIsDisplayed()
        rule.shot(folder, "19_audio_empty")
        back()

        // Tabs.
        clickText(s(R.string.nav_explore))
        rule.onNode(hasSetTextAction()).performTextInput("kahf")
        rule.onNodeWithText("18. Al-Kahf").assertIsDisplayed()
        rule.shot(folder, "20_explore_search")
        clickText(s(R.string.nav_saved))
        rule.onNodeWithText("Al-Faatiha 1:1").assertIsDisplayed()
        rule.shot(folder, "21_saved")
        clickText(s(R.string.nav_settings))
        rule.shot(folder, "22_settings")
        scrollTo(s(R.string.settings_advanced))
        clickText(s(R.string.settings_advanced))
        rule.onNodeWithText(s(R.string.sync_title)).assertIsDisplayed()
        rule.shot(folder, "23_settings_advanced")
        back()
        clickText(s(R.string.nav_home))
        scrollTo(s(R.string.home_next_prayer))
        rule.onNodeWithText(s(R.string.home_next_prayer)).assertIsDisplayed()

        // Accessibility: every clickable element is at least 40dp tall (Material minimum is 48dp touch area).
        val density = sc.let { var d = 1f; it.onActivity { a -> d = a.resources.displayMetrics.density }; d }
        val small = rule.onAllNodes(hasClickAction()).fetchSemanticsNodes()
            .filter { it.boundsInRoot.height / density < 40f && it.boundsInRoot.height > 0f }
        assertTrue("Small touch targets: ${small.map { it.config }}", small.isEmpty())
        sc.close()
    }
}
