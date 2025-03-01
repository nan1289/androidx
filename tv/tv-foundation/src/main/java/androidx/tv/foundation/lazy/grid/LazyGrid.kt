/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.tv.foundation.lazy.grid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.checkScrollableContainerConstraints
import androidx.compose.foundation.clipScrollableContainer
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.fastForEach
import androidx.tv.foundation.ExperimentalTvFoundationApi
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.layout.lazyLayoutSemantics
import androidx.tv.foundation.scrollableWithPivot

@Suppress("IllegalExperimentalApiUsage") // TODO (b/233188423): Address before moving to beta
@OptIn(ExperimentalFoundationApi::class, ExperimentalTvFoundationApi::class)
@Composable
internal fun LazyGrid(
    /** Modifier to be applied for the inner layout */
    modifier: Modifier = Modifier,
    /** State controlling the scroll position */
    state: TvLazyGridState,
    /** Prefix sums of cross axis sizes of slots per line, e.g. the columns for vertical grid. */
    slotSizesSums: Density.(Constraints) -> List<Int>,
    /** The inner padding to be added for the whole content (not for each individual item) */
    contentPadding: PaddingValues = PaddingValues(0.dp),
    /** reverse the direction of scrolling and layout */
    reverseLayout: Boolean = false,
    /** The layout orientation of the grid */
    isVertical: Boolean,
    /** Whether scrolling via the user gestures is allowed. */
    userScrollEnabled: Boolean,
    /** The vertical arrangement for items/lines. */
    verticalArrangement: Arrangement.Vertical,
    /** The horizontal arrangement for items/lines. */
    horizontalArrangement: Arrangement.Horizontal,
    /** offsets of child element within the parent and starting edge of the child from the pivot
     * defined by the parentOffset */
    pivotOffsets: PivotOffsets,
    /** The content of the grid */
    content: TvLazyGridScope.() -> Unit
) {
    val itemProvider = rememberLazyGridItemProvider(state, content)

    val semanticState = rememberLazyGridSemanticState(state, itemProvider, reverseLayout)

    val scope = rememberCoroutineScope()
    val placementAnimator = remember(state, isVertical) {
        LazyGridItemPlacementAnimator(scope, isVertical)
    }
    state.placementAnimator = placementAnimator

    val measurePolicy = rememberLazyGridMeasurePolicy(
        itemProvider,
        state,
        slotSizesSums,
        contentPadding,
        reverseLayout,
        isVertical,
        horizontalArrangement,
        verticalArrangement,
        placementAnimator
    )

    state.isVertical = isVertical

    ScrollPositionUpdater(itemProvider, state)

    val orientation = if (isVertical) Orientation.Vertical else Orientation.Horizontal
    LazyLayout(
        modifier = modifier
            .then(state.remeasurementModifier)
            .then(state.awaitLayoutModifier)
            .lazyLayoutSemantics(
                itemProvider = itemProvider,
                state = semanticState,
                orientation = orientation,
                userScrollEnabled = userScrollEnabled
            )
            .clipScrollableContainer(orientation)
            .scrollableWithPivot(
                orientation = orientation,
                reverseDirection = ScrollableDefaults.reverseDirection(
                    LocalLayoutDirection.current,
                    orientation,
                    reverseLayout
                ),
                state = state,
                enabled = userScrollEnabled,
                pivotOffsets = pivotOffsets
            ),
        prefetchState = state.prefetchState,
        measurePolicy = measurePolicy,
        itemProvider = itemProvider
    )
}

/** Extracted to minimize the recomposition scope */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScrollPositionUpdater(
    itemProvider: LazyGridItemProvider,
    state: TvLazyGridState
) {
    if (itemProvider.itemCount > 0) {
        state.updateScrollPositionIfTheFirstItemWasMoved(itemProvider)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun rememberLazyGridMeasurePolicy(
    /** Items provider of the list. */
    itemProvider: LazyGridItemProvider,
    /** The state of the list. */
    state: TvLazyGridState,
    /** Prefix sums of cross axis sizes of slots of the grid. */
    slotSizesSums: Density.(Constraints) -> List<Int>,
    /** The inner padding to be added for the whole content(nor for each individual item) */
    contentPadding: PaddingValues,
    /** reverse the direction of scrolling and layout */
    reverseLayout: Boolean,
    /** The layout orientation of the list */
    isVertical: Boolean,
    /** The horizontal arrangement for items. Required when isVertical is false */
    horizontalArrangement: Arrangement.Horizontal? = null,
    /** The vertical arrangement for items. Required when isVertical is true */
    verticalArrangement: Arrangement.Vertical? = null,
    /** Item placement animator. Should be notified with the measuring result */
    placementAnimator: LazyGridItemPlacementAnimator
) = remember<LazyLayoutMeasureScope.(Constraints) -> MeasureResult>(
    state,
    slotSizesSums,
    contentPadding,
    reverseLayout,
    isVertical,
    horizontalArrangement,
    verticalArrangement,
    placementAnimator
) {
    { containerConstraints ->
        checkScrollableContainerConstraints(
            containerConstraints,
            if (isVertical) Orientation.Vertical else Orientation.Horizontal
        )

        // resolve content paddings
        val startPadding =
            if (isVertical) {
                contentPadding.calculateLeftPadding(layoutDirection).roundToPx()
            } else {
                // in horizontal configuration, padding is reversed by placeRelative
                contentPadding.calculateStartPadding(layoutDirection).roundToPx()
            }

        val endPadding =
            if (isVertical) {
                contentPadding.calculateRightPadding(layoutDirection).roundToPx()
            } else {
                // in horizontal configuration, padding is reversed by placeRelative
                contentPadding.calculateEndPadding(layoutDirection).roundToPx()
            }
        val topPadding = contentPadding.calculateTopPadding().roundToPx()
        val bottomPadding = contentPadding.calculateBottomPadding().roundToPx()
        val totalVerticalPadding = topPadding + bottomPadding
        val totalHorizontalPadding = startPadding + endPadding
        val totalMainAxisPadding = if (isVertical) totalVerticalPadding else totalHorizontalPadding
        val beforeContentPadding = when {
            isVertical && !reverseLayout -> topPadding
            isVertical && reverseLayout -> bottomPadding
            !isVertical && !reverseLayout -> startPadding
            else -> endPadding // !isVertical && reverseLayout
        }
        val afterContentPadding = totalMainAxisPadding - beforeContentPadding
        val contentConstraints =
            containerConstraints.offset(-totalHorizontalPadding, -totalVerticalPadding)

        state.updateScrollPositionIfTheFirstItemWasMoved(itemProvider)

        val spanLayoutProvider = itemProvider.spanLayoutProvider
        val resolvedSlotSizesSums = slotSizesSums(containerConstraints)
        spanLayoutProvider.slotsPerLine = resolvedSlotSizesSums.size

        // Update the state's cached Density and slotsPerLine
        state.density = this
        state.slotsPerLine = resolvedSlotSizesSums.size

        val spaceBetweenLinesDp = if (isVertical) {
            requireNotNull(verticalArrangement).spacing
        } else {
            requireNotNull(horizontalArrangement).spacing
        }
        val spaceBetweenLines = spaceBetweenLinesDp.roundToPx()
        val spaceBetweenSlotsDp = if (isVertical) {
            horizontalArrangement?.spacing ?: 0.dp
        } else {
            verticalArrangement?.spacing ?: 0.dp
        }
        val spaceBetweenSlots = spaceBetweenSlotsDp.roundToPx()

        val itemsCount = itemProvider.itemCount

        // can be negative if the content padding is larger than the max size from constraints
        val mainAxisAvailableSize = if (isVertical) {
            containerConstraints.maxHeight - totalVerticalPadding
        } else {
            containerConstraints.maxWidth - totalHorizontalPadding
        }
        val visualItemOffset = if (!reverseLayout || mainAxisAvailableSize > 0) {
            IntOffset(startPadding, topPadding)
        } else {
            // When layout is reversed and paddings together take >100% of the available space,
            // layout size is coerced to 0 when positioning. To take that space into account,
            // we offset start padding by negative space between paddings.
            IntOffset(
                if (isVertical) startPadding else startPadding + mainAxisAvailableSize,
                if (isVertical) topPadding + mainAxisAvailableSize else topPadding
            )
        }

        val measuredItemProvider = LazyMeasuredItemProvider(
            itemProvider,
            this,
            spaceBetweenLines
        ) { index, key, crossAxisSize, mainAxisSpacing, placeables ->
            LazyMeasuredItem(
                index = index,
                key = key,
                isVertical = isVertical,
                crossAxisSize = crossAxisSize,
                mainAxisSpacing = mainAxisSpacing,
                reverseLayout = reverseLayout,
                layoutDirection = layoutDirection,
                beforeContentPadding = beforeContentPadding,
                afterContentPadding = afterContentPadding,
                visualOffset = visualItemOffset,
                placeables = placeables,
                placementAnimator = placementAnimator
            )
        }
        val measuredLineProvider = LazyMeasuredLineProvider(
            isVertical,
            resolvedSlotSizesSums,
            spaceBetweenSlots,
            itemsCount,
            spaceBetweenLines,
            measuredItemProvider,
            spanLayoutProvider
        ) { index, items, spans, mainAxisSpacing ->
            LazyMeasuredLine(
                index = index,
                items = items,
                spans = spans,
                isVertical = isVertical,
                slotsPerLine = resolvedSlotSizesSums.size,
                layoutDirection = layoutDirection,
                mainAxisSpacing = mainAxisSpacing,
                crossAxisSpacing = spaceBetweenSlots
            )
        }
        state.prefetchInfoRetriever = { line ->
            val lineConfiguration = spanLayoutProvider.getLineConfiguration(line.value)
            var index = ItemIndex(lineConfiguration.firstItemIndex)
            var slot = 0
            val result = ArrayList<Pair<Int, Constraints>>(lineConfiguration.spans.size)
            lineConfiguration.spans.fastForEach {
                val span = it.currentLineSpan
                result.add(index.value to measuredLineProvider.childConstraints(slot, span))
                ++index
                slot += span
            }
            result
        }

        val firstVisibleLineIndex: LineIndex
        val firstVisibleLineScrollOffset: Int
        Snapshot.withoutReadObservation {
            if (state.firstVisibleItemIndex < itemsCount || itemsCount <= 0) {
                firstVisibleLineIndex = spanLayoutProvider.getLineIndexOfItem(
                    state.firstVisibleItemIndex
                )
                firstVisibleLineScrollOffset = state.firstVisibleItemScrollOffset
            } else {
                // the data set has been updated and now we have less items that we were
                // scrolled to before
                firstVisibleLineIndex = spanLayoutProvider.getLineIndexOfItem(itemsCount - 1)
                firstVisibleLineScrollOffset = 0
            }
        }
        measureLazyGrid(
            itemsCount = itemsCount,
            measuredLineProvider = measuredLineProvider,
            measuredItemProvider = measuredItemProvider,
            mainAxisAvailableSize = mainAxisAvailableSize,
            beforeContentPadding = beforeContentPadding,
            afterContentPadding = afterContentPadding,
            spaceBetweenLines = spaceBetweenLines,
            firstVisibleLineIndex = firstVisibleLineIndex,
            firstVisibleLineScrollOffset = firstVisibleLineScrollOffset,
            scrollToBeConsumed = state.scrollToBeConsumed,
            constraints = contentConstraints,
            isVertical = isVertical,
            verticalArrangement = verticalArrangement,
            horizontalArrangement = horizontalArrangement,
            reverseLayout = reverseLayout,
            density = this,
            placementAnimator = placementAnimator,
            spanLayoutProvider = itemProvider.spanLayoutProvider,
            layout = { width, height, placement ->
                layout(
                    containerConstraints.constrainWidth(width + totalHorizontalPadding),
                    containerConstraints.constrainHeight(height + totalVerticalPadding),
                    emptyMap(),
                    placement
                )
            }
        ).also { state.applyMeasureResult(it) }
    }
}
