package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
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
        composeTestRule.onNodeWithTag("nav_home").assertIsSelected()
        composeTestRule.onNodeWithTag("quick_start_onboarding_card").assertIsDisplayed()

        // Studio -> Home
        composeTestRule.onNodeWithTag("nav_studio").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_studio").assertIsSelected()

        composeTestRule.onNodeWithTag("nav_home").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_home").assertIsSelected()
        composeTestRule.onNodeWithTag("quick_start_onboarding_card").assertIsDisplayed()

        // Player -> Home
        composeTestRule.onNodeWithTag("nav_player").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_player").assertIsSelected()

        composeTestRule.onNodeWithTag("nav_home").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_home").assertIsSelected()
        composeTestRule.onNodeWithTag("quick_start_onboarding_card").assertIsDisplayed()

        // Library -> Equalizer -> Home
        composeTestRule.onNodeWithTag("nav_library").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_library").assertIsSelected()

        composeTestRule.onNodeWithTag("nav_equalizer").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_equalizer").assertIsSelected()

        composeTestRule.onNodeWithTag("nav_home").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_home").assertIsSelected()
        composeTestRule.onNodeWithTag("quick_start_onboarding_card").assertIsDisplayed()

        // Permissions -> Home
        composeTestRule.onNodeWithTag("permissions_center_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("nav_home").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_home").assertIsSelected()
        composeTestRule.onNodeWithTag("quick_start_onboarding_card").assertIsDisplayed()
    }
}
