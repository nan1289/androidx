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

package androidx.window.embedding

import android.util.LayoutDirection.LOCALE
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.core.util.Preconditions.checkArgument
import androidx.core.util.Preconditions.checkArgumentNonnegative

/**
 * Split configuration rules for activity pairs. Define when activities that were launched on top of
 * each other should be shown side-by-side, and the visual properties of such splits. Can be set
 * either statically via [SplitController.Companion.initialize] or at runtime via
 * [SplitController.registerRule]. The rules can only be  applied to activities that
 * belong to the same application and are running in the same process. The rules are  always
 * applied only to activities that will be started  after the rules were set.
 */
class SplitPairRule : SplitRule {

    /**
     * Filters used to choose when to apply this rule. The rule may be used if any one of the
     * provided filters matches.
     */
    val filters: Set<SplitPairFilter>

    /**
     * Determines what happens with the primary container when all activities are finished in the
     * associated secondary container.
     * @see SplitRule.SplitFinishBehavior
     */
    @SplitFinishBehavior
    val finishPrimaryWithSecondary: Int

    /**
     * Determines what happens with the secondary container when all activities are finished in the
     * associated primary container.
     * @see SplitRule.SplitFinishBehavior
     */
    @SplitFinishBehavior
    val finishSecondaryWithPrimary: Int

    /**
     * If there is an existing split with the same primary container, indicates whether the
     * existing secondary container on top and all activities in it should be destroyed when a new
     * split is created using this rule. Otherwise the new secondary will appear on top by default.
     */
    val clearTop: Boolean

    internal constructor(
        filters: Set<SplitPairFilter>,
        @SplitFinishBehavior finishPrimaryWithSecondary: Int = FINISH_NEVER,
        @SplitFinishBehavior finishSecondaryWithPrimary: Int = FINISH_ALWAYS,
        clearTop: Boolean = false,
        @IntRange(from = 0) minWidthDp: Int = DEFAULT_SPLIT_MIN_DIMENSION_DP,
        @IntRange(from = 0) minSmallestWidthDp: Int = DEFAULT_SPLIT_MIN_DIMENSION_DP,
        @FloatRange(from = 0.0, to = 1.0) splitRatio: Float = 0.5f,
        @LayoutDirection layoutDirection: Int = LOCALE
    ) : super(minWidthDp, minSmallestWidthDp, splitRatio, layoutDirection) {
        checkArgumentNonnegative(minWidthDp, "minWidthDp must be non-negative")
        checkArgumentNonnegative(minSmallestWidthDp, "minSmallestWidthDp must be non-negative")
        checkArgument(splitRatio in 0.0..1.0, "splitRatio must be in 0.0..1.0 range")
        this.filters = filters.toSet()
        this.clearTop = clearTop
        this.finishPrimaryWithSecondary = finishPrimaryWithSecondary
        this.finishSecondaryWithPrimary = finishSecondaryWithPrimary
    }

    /**
     * Builder for [SplitPairRule].
     *
     * @param filters See [SplitPairRule.filters].
     */
    class Builder(
        private val filters: Set<SplitPairFilter>,
    ) {
        @IntRange(from = 0)
        private var minWidthDp: Int = DEFAULT_SPLIT_MIN_DIMENSION_DP
        @IntRange(from = 0)
        private var minSmallestWidthDp: Int = DEFAULT_SPLIT_MIN_DIMENSION_DP
        @SplitFinishBehavior
        private var finishPrimaryWithSecondary: Int = FINISH_NEVER
        @SplitFinishBehavior
        private var finishSecondaryWithPrimary: Int = FINISH_ALWAYS
        private var clearTop: Boolean = false
        @FloatRange(from = 0.0, to = 1.0)
        private var splitRatio: Float = 0.5f
        @LayoutDirection
        private var layoutDirection: Int = LOCALE

        /**
         * @see SplitPairRule.minWidthDp
         */
        fun setMinWidthDp(@IntRange(from = 0) minWidthDp: Int): Builder =
            apply { this.minWidthDp = minWidthDp }

        /**
         * @see SplitPairRule.minSmallestWidthDp
         */
        fun setMinSmallestWidthDp(@IntRange(from = 0) minSmallestWidthDp: Int): Builder =
            apply { this.minSmallestWidthDp = minSmallestWidthDp }

        /**
         * @see SplitPairRule.finishPrimaryWithSecondary
         */
        fun setFinishPrimaryWithSecondary(
            @SplitFinishBehavior finishPrimaryWithSecondary: Int
        ): Builder =
            apply { this.finishPrimaryWithSecondary = finishPrimaryWithSecondary }

        /**
         * @see SplitPairRule.finishSecondaryWithPrimary
         */
        fun setFinishSecondaryWithPrimary(
            @SplitFinishBehavior finishSecondaryWithPrimary: Int
        ): Builder =
            apply { this.finishSecondaryWithPrimary = finishSecondaryWithPrimary }

        /**
         * @see SplitPairRule.clearTop
         */
        @SuppressWarnings("MissingGetterMatchingBuilder")
        fun setClearTop(clearTop: Boolean): Builder =
            apply { this.clearTop = clearTop }

        /**
         * @see SplitPairRule.splitRatio
         */
        fun setSplitRatio(@FloatRange(from = 0.0, to = 1.0) splitRatio: Float): Builder =
            apply { this.splitRatio = splitRatio }

        /**
         * @see SplitPairRule.layoutDirection
         */
        fun setLayoutDirection(@LayoutDirection layoutDirection: Int): Builder =
            apply { this.layoutDirection = layoutDirection }

        fun build() = SplitPairRule(filters, finishPrimaryWithSecondary, finishSecondaryWithPrimary,
            clearTop, minWidthDp, minSmallestWidthDp, splitRatio, layoutDirection)
    }

    /**
     * Creates a new immutable instance by adding a filter to the set.
     * @see filters
     */
    internal operator fun plus(filter: SplitPairFilter): SplitPairRule {
        val newSet = mutableSetOf<SplitPairFilter>()
        newSet.addAll(filters)
        newSet.add(filter)
        return Builder(newSet.toSet())
            .setMinWidthDp(minWidthDp)
            .setMinSmallestWidthDp(minSmallestWidthDp)
            .setFinishPrimaryWithSecondary(finishPrimaryWithSecondary)
            .setFinishSecondaryWithPrimary(finishSecondaryWithPrimary)
            .setClearTop(clearTop)
            .setSplitRatio(splitRatio)
            .setLayoutDirection(layoutDirection)
            .build()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SplitPairRule) return false

        if (!super.equals(other)) return false
        if (filters != other.filters) return false
        if (finishPrimaryWithSecondary != other.finishPrimaryWithSecondary) return false
        if (finishSecondaryWithPrimary != other.finishSecondaryWithPrimary) return false
        if (clearTop != other.clearTop) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + filters.hashCode()
        result = 31 * result + finishPrimaryWithSecondary.hashCode()
        result = 31 * result + finishSecondaryWithPrimary.hashCode()
        result = 31 * result + clearTop.hashCode()
        return result
    }
}