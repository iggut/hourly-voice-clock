package com.hourlyvoiceclock.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(RobolectricTestRunner::class)
class DashboardCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dashboardCard_mergesSemantics() {
        var clicked = false
        composeTestRule.setContent {
            DashboardCard(
                title = "Test Title",
                subtitle = "Test Subtitle",
                icon = Icons.Filled.Home,
                onClick = { clicked = true }
            )
        }

        // Verify that the components are displayed
        composeTestRule.onNodeWithText("Test Title").assertIsDisplayed()

        // When semantics are merged, the parent node will combine the text of all children.
        // We can test that clicking the node with the title text triggers the click.
        composeTestRule.onNode(hasText("Test Title", substring = true) and hasText("Test Subtitle", substring = true), useUnmergedTree = false).assertIsDisplayed()

        composeTestRule.onNodeWithText("Test Title").performClick()
        assert(clicked) { "DashboardCard was not clicked" }
    }
}
