package com.noor.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.noor.app.TestSupport.shot
import com.noor.app.TestSupport.str
import com.noor.app.data.prefs.ThemeMode
import com.noor.app.util.AppLanguage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Light theme on a small phone. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h640dp-xhdpi")
class SmallPhoneLightTest {
    @get:Rule val rule = createEmptyComposeRule()

    @Test
    fun smallPhoneLight() {
        TestSupport.prepare(AppLanguage.ENGLISH, ThemeMode.LIGHT)
        val sc = TestSupport.launch()
        rule.onAllNodesWithText(sc.str(R.string.prayer_choose_city)).onFirst().performClick()
        rule.onNode(androidx.compose.ui.test.hasSetTextAction()).performTextInput("London")
        rule.onAllNodesWithText("United Kingdom").onFirst().performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText(sc.str(R.string.home_next_prayer)).fetchSemanticsNodes().isNotEmpty() }
        rule.shot("en-light-small", "01_home")
        rule.onAllNodesWithText(sc.str(R.string.nav_settings)).onFirst().performClick()
        rule.onNodeWithText(sc.str(R.string.settings_language)).assertIsDisplayed()
        rule.shot("en-light-small", "02_settings")
        sc.close()
    }
}

/** Tablet layout, Arabic, light. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w800dp-h1280dp-xhdpi")
class TabletTest {
    @get:Rule val rule = createEmptyComposeRule()

    @Test
    fun tablet() {
        TestSupport.prepare(AppLanguage.ARABIC, ThemeMode.LIGHT)
        val sc = TestSupport.launch()
        rule.onAllNodesWithText(sc.str(R.string.prayer_choose_city)).onFirst().performClick()
        rule.onAllNodesWithText("قم المقدسة").onFirst().performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText(sc.str(R.string.home_next_prayer)).fetchSemanticsNodes().isNotEmpty() }
        rule.shot("ar-light-tablet", "01_home")
        rule.onAllNodesWithText(sc.str(R.string.nav_explore)).onFirst().performClick()
        rule.shot("ar-light-tablet", "02_explore")
        sc.close()
    }
}
