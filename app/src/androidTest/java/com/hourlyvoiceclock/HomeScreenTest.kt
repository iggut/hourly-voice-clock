package com.hourlyvoiceclock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hourlyvoiceclock.ui.home.HomeScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homeScreen_displaysCurrentTime() {
        composeTestRule.setContent {
            HomeScreen(
                onNavigateToVoiceSettings = {},
                onNavigateToFormatSettings = {},
                onNavigateToScheduleSettings = {}
            )
        }
        composeTestRule.onNodeWithText("Announce time now").assertIsDisplayed()
    }

    @Test
    fun homeScreen_togglesHourlyAnnouncements() {
        composeTestRule.setContent {
            HomeScreen(
                onNavigateToVoiceSettings = {},
                onNavigateToFormatSettings = {},
                onNavigateToScheduleSettings = {}
            )
        }
        composeTestRule.onNodeWithText("Hourly announcements").assertIsDisplayed()
    }

    @Test
    fun homeScreen_hourlyAnnouncementsIcon_doesNotHaveRedundantContentDescription() {
        composeTestRule.setContent {
            HomeScreen(
                onNavigateToVoiceSettings = {},
                onNavigateToFormatSettings = {},
                onNavigateToScheduleSettings = {}
            )
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val announcementsText = context.getString(R.string.hourly_announcements)

        // Find the node that has the redundant content description (if it exists)
        val redundantNodes = composeTestRule.onAllNodes(
            hasContentDescription(announcementsText),
            useUnmergedTree = true
        )

        // Assert that no unmerged node carries the explicit content description since it should be null
        // to avoid screen reader double announcements
        redundantNodes.assertCountEquals(0)
    }
}
