package com.freeteaspoon.wppenhacer.ui.miuix

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.utils.springAnimateToPage

/** Keeps selection pinned to the requested destination during cancellable pager retargeting. */
@Stable
internal class PrimaryPagerNavigationState(
    val pagerState: PagerState,
    private val scope: CoroutineScope,
    private val animationsEnabled: Boolean,
    private val rootTabBack: RootTabBackState,
) {
    var selectedPage by mutableIntStateOf(pagerState.currentPage)
        private set
    private var navigatingTo by mutableStateOf<Int?>(null)
    private var request = 0
    private var job: Job? = null

    fun navigateTo(page: Int) {
        if (page !in 0 until pagerState.pageCount) return
        if (page == selectedPage && page == pagerState.currentPage) return
        val currentRequest = ++request
        job?.cancel()
        selectedPage = page
        rootTabBack.targetPage.value = page
        navigatingTo = page
        job = scope.launch {
            try {
                if (animationsEnabled) {
                    pagerState.springAnimateToPage(page)
                } else pagerState.scrollToPage(page)
            } finally {
                if (currentRequest == request) {
                    navigatingTo = null
                    selectedPage = pagerState.currentPage
                    rootTabBack.targetPage.value = selectedPage
                    job = null
                }
            }
        }
    }

    fun syncPage() {
        if (navigatingTo == null) {
            selectedPage = pagerState.currentPage
            rootTabBack.targetPage.value = selectedPage
        }
    }

    fun dispose() {
        request++
        job?.cancel()
        job = null
        navigatingTo = null
    }
}

@Composable
internal fun rememberPrimaryPagerNavigationState(
    pagerState: PagerState,
    animationsEnabled: Boolean,
    rootTabBack: RootTabBackState,
    scope: CoroutineScope = rememberCoroutineScope(),
): PrimaryPagerNavigationState = remember(pagerState, animationsEnabled, rootTabBack, scope) {
    PrimaryPagerNavigationState(pagerState, scope, animationsEnabled, rootTabBack)
}
