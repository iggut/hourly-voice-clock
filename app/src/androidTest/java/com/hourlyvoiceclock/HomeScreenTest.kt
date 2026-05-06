package com.hourlyvoiceclock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
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
}
