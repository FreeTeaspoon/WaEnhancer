package com.wmods.wppenhacer.ui.miuix

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wmods.wppenhacer.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManagerShellTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MiuixMainActivity>()

    @Test
    fun threePrimaryDestinationsAreReachable() {
        val activity = composeRule.activity
        PrimaryDestination.entries.zip(
            listOf(
                R.string.manager_home,
                R.string.manager_features,
                R.string.manager_tools,
            ),
        ).forEach { (destination, resource) ->
            val label = activity.getString(resource)
            val navigationItem = composeRule.onNodeWithTag(primaryNavigationTag(destination.ordinal), useUnmergedTree = true)
            navigationItem.performClick()
            composeRule.waitForIdle()
            composeRule.onAllNodesWithText(label, useUnmergedTree = true).onFirst().assertIsDisplayed()
        }
    }

    @Test
    fun featuresTabKeepsSearchInThePage() {
        val activity = composeRule.activity
        composeRule.onNodeWithTag(primaryNavigationTag(PrimaryDestination.FEATURES.ordinal), useUnmergedTree = true).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(activity.getString(R.string.manager_search_features), useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText(activity.getString(R.string.manager_customize), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun featuresSearchFindsPreferencesFromOtherPages() {
        val activity = composeRule.activity
        composeRule.onNodeWithTag(primaryNavigationTag(PrimaryDestination.FEATURES.ordinal), useUnmergedTree = true).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("features-search", useUnmergedTree = true).performTextInput("block calls")
        composeRule.waitForIdle()

        composeRule.onNodeWithText(activity.getString(R.string.call_blocker), useUnmergedTree = true).assertIsDisplayed()
    }
}
