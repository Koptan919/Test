package com.noor.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.noor.app.TestSupport.shot
import com.noor.app.data.remote.GitHubClient
import com.noor.app.ui.navigation.UpdateDialog
import com.noor.app.ui.theme.NoorTheme
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The Arabic update dialog shows the changelog and opens the official APK download. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "ar-w411dp-h891dp-xxhdpi")
class UpdateDialogTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun arabicUpdateDialog() {
        val release = GitHubClient.parseRelease(
            JSONObject(
                """{"tag_name":"v0.3.0","name":"Noor 0.3.0","html_url":"https://github.com/Koptan919/noor-releases/releases/tag/v0.3.0",
                   "body":"- تحسينات في قارئ القرآن\n- إصلاحات في التنبيهات","prerelease":false,"draft":false,
                   "assets":[{"name":"noor-v0.3.0.apk","browser_download_url":"https://github.com/Koptan919/noor-releases/releases/download/v0.3.0/noor-v0.3.0.apk","size":1}]}""",
            ),
        )
        var later = false
        var updated = false
        rule.setContent { NoorTheme(darkTheme = true, reduceMotion = true) { UpdateDialog(release, onUpdate = { updated = true }, onLater = { later = true }) } }
        rule.onNodeWithText("إصدار جديد من نور متاح").assertIsDisplayed()
        rule.onNodeWithText("الإصدار ٠.٣.٠").assertIsDisplayed()
        rule.shot("ar-dark-phone", "10_update_dialog")
        rule.onNodeWithText("تحديث").performClick()
        assertEquals("Update starts the verified install flow", true, updated)
        rule.onNodeWithText("لاحقاً").performClick()
        assertEquals(true, later)
    }
}
