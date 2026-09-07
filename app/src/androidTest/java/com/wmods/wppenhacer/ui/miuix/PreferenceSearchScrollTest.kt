package com.wmods.wppenhacer.ui.miuix

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text

class PreferenceSearchScrollTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun searchShowsFirstMiddleAndLastRowsAfterPreferencesLoad() {
        val target = mutableStateOf("row-0")
        val state = mutableStateOf(ManagerUiState())
        composeRule.setContent {
            val context = LocalContext.current
            ManagerTheme(ManagerAppearanceSettings()) {
                PreferencePageScreen(
                    source = PreferenceSource.GENERAL,
                    state = state.value,
                    controller = PreferenceController(
                        context,
                        context.getSharedPreferences("search-scroll-test", Context.MODE_PRIVATE),
                    ),
                    wide = false,
                    bottomPadding = 0.dp,
                    onBack = {},
                    onNavigate = {},
                    highlightKey = target.value,
                )
            }
        }
        composeRule.runOnIdle {
            state.value = ManagerUiState(specs = List(60) { index ->
                PreferenceSpec(
                    key = "row-$index",
                    source = PreferenceSource.GENERAL,
                    category = "Group ${index / 20}",
                    title = "Setting $index",
                    summary = if (index % 3 == 0) "A longer summary\nwith a second line\nand a third line" else null,
                    kind = PreferenceKind.SWITCH,
                )
            })
        }
        // Repeated selections include moving backwards and crossing group headings.
        listOf(0, 10, 30, 59, 20, 0).forEach { index ->
            composeRule.runOnIdle { target.value = "row-$index" }
            composeRule.onNodeWithText("● Setting $index", useUnmergedTree = true).assertIsDisplayed()
        }
    }

    @Test
    fun directAndRestoredScrollPositionsCollapseBarAndEnableBlur() {
        lateinit var listState: LazyListState
        lateinit var behavior: ScrollBehavior
        composeRule.setContent {
            listState = rememberLazyListState(initialFirstVisibleItemIndex = 20)
            behavior = rememberManagerListScrollBehavior(listState)
            // Supply the measured collapse range without relying on screen density.
            behavior.state.heightOffsetLimit = -100f
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                items(60) { Text("Row $it", modifier = Modifier.height(80.dp)) }
            }
        }
        fun assertScrolled() = composeRule.runOnIdle {
            assertEquals(1f, behavior.state.collapsedFraction, 0f)
            assertTrue(behavior.state.contentOffset <= -100f)
        }
        assertScrolled()
        runBlocking { listState.scrollToItem(0) }
        composeRule.runOnIdle { assertEquals(0f, behavior.state.contentOffset, 0f) }
        runBlocking { listState.scrollToItem(35) }
        assertScrolled()
    }
}
