package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NavigationBugTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `navigate away from home to every tab and return cleanly to home`() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_scan_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_import_btn").assertIsDisplayed()

        // Studio -> Home
        composeTestRule.onNodeWithTag("nav_studio").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("studio_prompt_input").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_home").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_scan_btn").assertIsDisplayed()

        // Player -> Home
        composeTestRule.onNodeWithTag("nav_player").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("player_open_library_btn").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_home").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_scan_btn").assertIsDisplayed()

        // Library -> Equalizer -> Home
        composeTestRule.onNodeWithTag("nav_library").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("saf_import_audio_btn").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_equalizer").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("eq_preset_flat").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_home").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_scan_btn").assertIsDisplayed()

        // Permissions -> Home
        composeTestRule.onNodeWithTag("permissions_center_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("open_app_settings_btn").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_home").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_scan_btn").assertIsDisplayed()
    }
}
