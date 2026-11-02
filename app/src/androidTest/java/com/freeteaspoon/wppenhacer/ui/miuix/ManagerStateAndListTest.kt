package com.freeteaspoon.wppenhacer.ui.miuix

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.freeteaspoon.wppenhacer.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text

class ManagerStateAndListTest {
    @get:Rule val rule = createComposeRule()

    @Test fun consecutiveInstantOperationsCannotLeaveTheirHostLocked() {
        val operation = ManagerOperation()
        val completed = kotlinx.coroutines.CompletableDeferred<Unit>()
        rule.setContent {
            ManagerTheme(ManagerAppearanceSettings()) {
                Scaffold { _ -> ManagerOperationHost(operation) }
                LaunchedEffect(Unit) {
                    repeat(3) { operation.run("Saving") {} }
                    completed.complete(Unit)
                }
            }
        }
        rule.waitUntil(5000) { completed.isCompleted }
        rule.runOnIdle { assertFalse(operation.busy.value) }
    }

    @Test fun growingGroupsComposeOnlyVisibleRowsAndKeepTheirAnchorAfterRemoval() {
        val rows = mutableStateOf((0 until 500).toList())
        val composed = mutableSetOf<Int>()
        lateinit var state: LazyListState
        lateinit var scope: CoroutineScope
        rule.setContent {
            state = rememberLazyListState(initialFirstVisibleItemIndex = 250)
            scope = rememberCoroutineScope()
            ManagerTheme(ManagerAppearanceSettings()) {
                LazyColumn(state = state, modifier = Modifier.fillMaxSize()) {
                    managerGroupedCardItems("large-group", rows.value.map { id ->
                        ManagerCardItem("$id") {
                            DisposableEffect(id) {
                                composed.add(id)
                                onDispose { composed.remove(id) }
                            }
                            Text("Row $id", Modifier.height(60.dp))
                        }
                    })
                }
            }
        }
        rule.runOnIdle {
            assertTrue("Off-screen rows must remain lazy", composed.size < 50)
            assertEquals("large-group:250", state.layoutInfo.visibleItemsInfo.first().key)
            rows.value = rows.value.drop(100)
        }
        rule.waitForIdle()
        rule.runOnIdle {
            assertEquals("large-group:250", state.layoutInfo.visibleItemsInfo.first().key)
            scope.launch { state.scrollToItem(rows.value.lastIndex) }
        }
        rule.onNodeWithText("Row 499").assertIsDisplayed()
    }

    @Test fun errorHintAndRetryHaveSeparateAccessibleTargets() {
        var retried = false
        var retryLabel = ""
        rule.setContent {
            retryLabel = androidx.compose.ui.res.stringResource(R.string.manager_try_again)
            ManagerTheme(ManagerAppearanceSettings()) {
                ManagerPageStateHost(ManagerPageStateKind.ERROR, "Unable to load", "Check access",
                    Modifier.fillMaxSize(), onRetry = { retried = true }, content = {})
            }
        }
        rule.onNodeWithContentDescription("Unable to load. Check access").assertHasNoClickAction()
        rule.onNodeWithText("Check access", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText(retryLabel).assertHasClickAction().performClick()
        rule.runOnIdle { assertTrue(retried) }
    }

    @Test fun searchKeepsFocusWhenResultsChangeToEmptyAndBack() {
        val query = mutableStateOf("")
        rule.setContent {
            ManagerTheme(ManagerAppearanceSettings()) {
                ManagerSearchContent(ManagerUiState(specs = listOf(
                    PreferenceSpec("sample", PreferenceSource.GENERAL, "General", "Sample", null, PreferenceKind.SWITCH)
                )), query.value, { query.value = it }, {}, "Features", false, null)
            }
        }
        rule.onNodeWithTag("features-search").performClick().performTextInput("unmatched")
        rule.onNodeWithTag("features-search").assertIsFocused().performTextReplacement("sample")
        rule.onNodeWithTag("features-search").assertIsFocused()
        rule.onNodeWithText("Sample").assertIsDisplayed()
    }

    @Test fun initialLoadingRejectsPullsAndEmptyResultsRemainVisibleDuringRefresh() {
        val state = mutableStateOf(ManagerPageStateKind.LOADING)
        val refreshing = mutableStateOf(false)
        var calls = 0
        rule.setContent {
            ManagerTheme(ManagerAppearanceSettings()) {
                ManagerDetailScaffold("Recordings", false, null,
                    pageState = state.value, stateTitle = "No recordings", stateMessage = "Record a call to see it here",
                    isRefreshing = refreshing.value, onRefresh = { calls++; refreshing.value = true }) {}
            }
        }
        rule.onRoot().performTouchInput { swipeDown() }
        rule.runOnIdle { assertEquals(0, calls); state.value = ManagerPageStateKind.EMPTY }
        rule.waitForIdle()
        rule.onRoot().performTouchInput { swipeDown() }
        rule.runOnIdle { assertEquals(1, calls); assertTrue(refreshing.value) }
        rule.onNodeWithText("No recordings", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("Record a call to see it here", useUnmergedTree = true).assertIsDisplayed()
    }
}
