/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.foundation.pager

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(Parameterized::class)
internal class PagerStateTest(val config: ParamConfig) : BasePagerTest(config) {

    @Test
    fun scrollToPage_shouldPlacePagesCorrectly() {
        // Arrange
        val state = PagerState()
        createPager(state = state, modifier = Modifier.fillMaxSize())

        // Act and Assert
        repeat(DefaultPageCount) {
            assertThat(state.currentPage).isEqualTo(it)
            rule.runOnIdle {
                scope.launch {
                    state.scrollToPage(state.currentPage + 1)
                }
            }
            confirmPageIsInCorrectPosition(state.currentPage)
        }
    }

    @Test
    fun scrollToPage_longSkipShouldNotPlaceIntermediatePages() {
        // Arrange
        val state = PagerState()
        createPager(state = state, modifier = Modifier.fillMaxSize())

        // Act
        assertThat(state.currentPage).isEqualTo(0)
        rule.runOnIdle {
            scope.launch {
                state.scrollToPage(DefaultPageCount - 1)
            }
        }

        // Assert
        rule.runOnIdle {
            assertThat(state.currentPage).isEqualTo(DefaultPageCount - 1)
            assertThat(placed).doesNotContain(DefaultPageCount / 2 - 1)
            assertThat(placed).doesNotContain(DefaultPageCount / 2)
            assertThat(placed).doesNotContain(DefaultPageCount / 2 + 1)
        }
        confirmPageIsInCorrectPosition(state.currentPage)
    }

    @Test
    fun animateScrollToPage_shouldPlacePagesCorrectly() {
        // Arrange
        val state = PagerState()
        createPager(state = state, modifier = Modifier.fillMaxSize())

        // Act and Assert
        repeat(DefaultPageCount) {
            assertThat(state.currentPage).isEqualTo(it)
            rule.runOnIdle {
                scope.launch {
                    state.animateScrollToPage(state.currentPage + 1)
                }
            }
            rule.waitForIdle()
            confirmPageIsInCorrectPosition(state.currentPage)
        }
    }

    @Test
    fun animateScrollToPage_longSkipShouldNotPlaceIntermediatePages() {
        // Arrange
        val state = PagerState()
        createPager(state = state, modifier = Modifier.fillMaxSize())

        // Act
        assertThat(state.currentPage).isEqualTo(0)
        rule.runOnIdle {
            scope.launch {
                state.animateScrollToPage(DefaultPageCount - 1)
            }
        }

        // Assert
        rule.runOnIdle {
            assertThat(state.currentPage).isEqualTo(DefaultPageCount - 1)
            assertThat(placed).doesNotContain(DefaultPageCount / 2 - 1)
            assertThat(placed).doesNotContain(DefaultPageCount / 2)
            assertThat(placed).doesNotContain(DefaultPageCount / 2 + 1)
        }
        confirmPageIsInCorrectPosition(state.currentPage)
    }

    @Test
    fun scrollToPage_shouldCoerceWithinRange() {
        // Arrange
        val state = PagerState()
        createPager(state = state, modifier = Modifier.fillMaxSize())

        // Act
        assertThat(state.currentPage).isEqualTo(0)
        rule.runOnIdle {
            scope.launch {
                state.scrollToPage(DefaultPageCount)
            }
        }

        // Assert
        rule.runOnIdle { assertThat(state.currentPage).isEqualTo(DefaultPageCount - 1) }

        // Act
        rule.runOnIdle {
            scope.launch {
                state.scrollToPage(-1)
            }
        }

        // Assert
        rule.runOnIdle { assertThat(state.currentPage).isEqualTo(0) }
    }

    @Test
    fun animateScrollToPage_shouldCoerceWithinRange() {
        // Arrange
        val state = PagerState()
        createPager(state = state, modifier = Modifier.fillMaxSize())

        // Act
        assertThat(state.currentPage).isEqualTo(0)
        rule.runOnIdle {
            scope.launch {
                state.animateScrollToPage(DefaultPageCount)
            }
        }

        // Assert
        rule.runOnIdle { assertThat(state.currentPage).isEqualTo(DefaultPageCount - 1) }

        // Act
        rule.runOnIdle {
            scope.launch {
                state.animateScrollToPage(-1)
            }
        }

        // Assert
        rule.runOnIdle { assertThat(state.currentPage).isEqualTo(0) }
    }

    @Test
    fun animateScrollToPage_withPassedAnimation() {
        // Arrange
        val state = PagerState()
        createPager(state = state, modifier = Modifier.fillMaxSize())
        val differentAnimation: AnimationSpec<Float> = tween()

        // Act and Assert
        repeat(DefaultPageCount) {
            assertThat(state.currentPage).isEqualTo(it)
            rule.runOnIdle {
                scope.launch {
                    state.animateScrollToPage(state.currentPage + 1, differentAnimation)
                }
            }
            rule.waitForIdle()
            confirmPageIsInCorrectPosition(state.currentPage)
        }
    }

    @Test
    fun currentPage_shouldChangeWhenClosestPageToSnappedPositionChanges() {
        // Arrange
        val state = PagerState()
        createPager(state = state, modifier = Modifier.fillMaxSize())
        var previousCurrentPage = state.currentPage

        // Act
        // Move less than half an item
        val firstDelta = (pagerSize * 0.4f) * scrollForwardSign
        onPager().performTouchInput {
            down(layoutStart)
            if (isVertical) {
                moveBy(Offset(0f, firstDelta))
            } else {
                moveBy(Offset(firstDelta, 0f))
            }
        }

        // Assert
        rule.runOnIdle {
            assertThat(state.currentPage).isEqualTo(previousCurrentPage)
        }
        // Release pointer
        onPager().performTouchInput { up() }

        rule.runOnIdle {
            previousCurrentPage = state.currentPage
        }
        confirmPageIsInCorrectPosition(state.currentPage)

        // Arrange
        // Pass closest to snap position threshold (over half an item)
        val secondDelta = (pagerSize * 0.6f) * scrollForwardSign

        // Act
        onPager().performTouchInput {
            down(layoutStart)
            if (isVertical) {
                moveBy(Offset(0f, secondDelta))
            } else {
                moveBy(Offset(secondDelta, 0f))
            }
        }

        // Assert
        rule.runOnIdle {
            assertThat(state.currentPage).isEqualTo(previousCurrentPage + 1)
        }

        onPager().performTouchInput { up() }
        rule.waitForIdle()
        confirmPageIsInCorrectPosition(state.currentPage)
    }

    @Test
    fun targetPage_performScroll_shouldShowNextPage() {
        // Arrange
        val state = PagerState()
        createPager(
            state = state,
            modifier = Modifier.fillMaxSize(),
            snappingPage = PagerSnapDistance.atMost(3)
        )
        rule.runOnIdle { assertThat(state.targetPage).isEqualTo(state.currentPage) }

        rule.mainClock.autoAdvance = false
        // Act
        // Moving forward
        val forwardDelta = pagerSize * 0.4f * scrollForwardSign.toFloat()
        onPager().performTouchInput {
            down(layoutStart)
            moveBy(Offset(forwardDelta, forwardDelta))
        }

        // Assert
        assertThat(state.targetPage).isEqualTo(state.currentPage + 1)
        assertThat(state.targetPage).isNotEqualTo(state.currentPage)

        // Reset
        rule.mainClock.autoAdvance = true
        onPager().performTouchInput { up() }
        rule.runOnIdle { assertThat(state.targetPage).isEqualTo(state.currentPage) }
        rule.runOnIdle {
            runBlocking { state.scrollToPage(5) }
        }

        rule.mainClock.autoAdvance = false
        // Act
        // Moving backward
        val backwardDelta = -pagerSize * 0.4f * scrollForwardSign.toFloat()
        onPager().performTouchInput {
            down(layoutEnd)
            moveBy(Offset(backwardDelta, backwardDelta))
        }

        // Assert
        assertThat(state.targetPage).isEqualTo(state.currentPage - 1)
        assertThat(state.targetPage).isNotEqualTo(state.currentPage)

        rule.mainClock.autoAdvance = true
        onPager().performTouchInput { up() }
        rule.runOnIdle { assertThat(state.targetPage).isEqualTo(state.currentPage) }
    }

    @Test
    fun targetPage_performingFling_shouldGoToPredictedPage() {
        // Arrange
        val state = PagerState()
        createPager(
            state = state,
            modifier = Modifier.fillMaxSize(),
            snappingPage = PagerSnapDistance.atMost(3)
        )
        rule.runOnIdle { assertThat(state.targetPage).isEqualTo(state.currentPage) }

        rule.mainClock.autoAdvance = false
        // Act
        // Moving forward
        var previousTarget = state.targetPage
        val forwardDelta = pagerSize * scrollForwardSign.toFloat()
        onPager().performTouchInput {
            swipeWithVelocityAcrossMainAxis(20000f, forwardDelta)
        }
        rule.mainClock.advanceTimeUntil { state.targetPage != previousTarget }

        // Assert
        assertThat(state.targetPage).isEqualTo(state.currentPage + 3)
        assertThat(state.targetPage).isNotEqualTo(state.currentPage)

        rule.mainClock.autoAdvance = true
        rule.runOnIdle { assertThat(state.targetPage).isEqualTo(state.currentPage) }
        rule.mainClock.autoAdvance = false
        // Act
        // Moving backward
        previousTarget = state.targetPage
        val backwardDelta = -pagerSize * scrollForwardSign.toFloat()
        onPager().performTouchInput {
            swipeWithVelocityAcrossMainAxis(20000f, backwardDelta)
        }
        rule.mainClock.advanceTimeUntil { state.targetPage != previousTarget }

        // Assert
        assertThat(state.targetPage).isEqualTo(state.currentPage - 3)
        assertThat(state.targetPage).isNotEqualTo(state.currentPage)

        rule.mainClock.autoAdvance = true
        rule.runOnIdle { assertThat(state.targetPage).isEqualTo(state.currentPage) }
    }

    @Test
    fun targetPage_shouldReflectTargetWithAnimation() {
        // Arrange
        val state = PagerState()
        createPager(
            state = state,
            modifier = Modifier.fillMaxSize()
        )
        rule.runOnIdle { assertThat(state.targetPage).isEqualTo(state.currentPage) }

        rule.mainClock.autoAdvance = false
        // Act
        // Moving forward
        var previousTarget = state.targetPage
        rule.runOnIdle {
            scope.launch {
                state.animateScrollToPage(DefaultPageCount - 1)
            }
        }
        rule.mainClock.advanceTimeUntil { state.targetPage != previousTarget }

        // Assert
        assertThat(state.targetPage).isEqualTo(DefaultPageCount - 1)
        assertThat(state.targetPage).isNotEqualTo(state.currentPage)

        rule.mainClock.autoAdvance = true
        rule.runOnIdle { assertThat(state.targetPage).isEqualTo(state.currentPage) }
        rule.mainClock.autoAdvance = false

        // Act
        // Moving backward
        previousTarget = state.targetPage
        rule.runOnIdle {
            scope.launch {
                state.animateScrollToPage(0)
            }
        }
        rule.mainClock.advanceTimeUntil { state.targetPage != previousTarget }

        // Assert
        assertThat(state.targetPage).isEqualTo(0)
        assertThat(state.targetPage).isNotEqualTo(state.currentPage)

        rule.mainClock.autoAdvance = true
        rule.runOnIdle { assertThat(state.targetPage).isEqualTo(state.currentPage) }
    }

    @Test
    fun currentPageOffset_shouldReflectScrollingOfCurrentPage() {
        // Arrange
        val state = PagerState(DefaultPageCount / 2)
        createPager(state = state, modifier = Modifier.fillMaxSize())

        // No offset initially
        rule.runOnIdle {
            assertThat(state.currentPageOffset).isWithin(0.01f).of(0f)
        }

        // Act
        // Moving forward
        onPager().performTouchInput {
            down(layoutStart)
            if (isVertical) {
                moveBy(Offset(0f, scrollForwardSign * pagerSize / 4f))
            } else {
                moveBy(Offset(scrollForwardSign * pagerSize / 4f, 0f))
            }
        }

        rule.runOnIdle {
            assertThat(state.currentPageOffset).isWithin(0.1f).of(0.25f)
        }

        onPager().performTouchInput { up() }
        rule.waitForIdle()

        // Reset
        rule.runOnIdle {
            scope.launch {
                state.scrollToPage(DefaultPageCount / 2)
            }
        }

        // No offset initially (again)
        rule.runOnIdle {
            assertThat(state.currentPageOffset).isWithin(0.01f).of(0f)
        }

        // Act
        // Moving backward
        onPager().performTouchInput {
            down(layoutStart)
            if (isVertical) {
                moveBy(Offset(0f, -scrollForwardSign * pagerSize / 4f))
            } else {
                moveBy(Offset(-scrollForwardSign * pagerSize / 4f, 0f))
            }
        }

        rule.runOnIdle {
            assertThat(state.currentPageOffset).isWithin(0.1f).of(-0.25f)
        }
    }

    @Test
    fun initialPageOnPagerState_shouldDisplayThatPageFirst() {
        // Arrange
        val state = PagerState(5)

        // Act
        createPager(state = state, modifier = Modifier.fillMaxSize())

        // Assert
        rule.onNodeWithTag("4").assertDoesNotExist()
        rule.onNodeWithTag("5").assertIsDisplayed()
        rule.onNodeWithTag("6").assertDoesNotExist()
        confirmPageIsInCorrectPosition(state.currentPage)
    }

    @Test
    fun testStateRestoration() {
        // Arrange
        val tester = StateRestorationTester(rule)
        val state = PagerState()
        tester.setContent {
            scope = rememberCoroutineScope()
            HorizontalOrVerticalPager(
                state = state,
                pageCount = DefaultPageCount,
                modifier = Modifier.fillMaxSize()
            ) {
                Page(it)
            }
        }

        // Act
        rule.runOnIdle {
            scope.launch {
                state.scrollToPage(5)
            }
            runBlocking {
                state.scroll {
                    scrollBy(50f)
                }
            }
        }

        // TODO(levima) Update to assert offset as well
        val previousPage = state.currentPage
        tester.emulateSavedInstanceStateRestore()

        // Assert
        rule.runOnIdle {
            assertThat(state.currentPage).isEqualTo(previousPage)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = mutableListOf<ParamConfig>().apply {
            for (orientation in TestOrientation) {
                for (reverseLayout in TestReverseLayout) {
                    for (layoutDirection in TestLayoutDirection) {
                        add(
                            ParamConfig(
                                orientation = orientation,
                                reverseLayout = reverseLayout,
                                layoutDirection = layoutDirection
                            )
                        )
                    }
                }
            }
        }
    }
}