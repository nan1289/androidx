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

package androidx.tv.tvmaterial.samples

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material.ExperimentalTvMaterialApi
import androidx.tv.material.immersivelist.ImmersiveList

@OptIn(ExperimentalTvMaterialApi::class)
@Composable
fun SampleImmersiveList() {
    val immersiveListHeight = 300.dp
    val cardSpacing = 10.dp
    val cardWidth = 200.dp
    val cardHeight = 150.dp
    val backgrounds = listOf(
        Color.Red,
        Color.Blue,
        Color.Magenta,
    )

    Box(
        modifier = Modifier
            .height(immersiveListHeight + cardHeight / 2)
            .fillMaxWidth()
    ) {
        ImmersiveList(
            modifier = Modifier
                .height(immersiveListHeight)
                .fillMaxWidth(),
            listAlignment = Alignment.BottomEnd,
            background = { index, _ ->
                Box(
                    modifier = Modifier
                        .background(backgrounds[index].copy(alpha = 0.3f))
                        .fillMaxSize()
                )
            }
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(cardSpacing),
                modifier = Modifier.offset(y = cardHeight / 2)
            ) {
                backgrounds.forEachIndexed { index, backgroundColor ->
                    var isFocused by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .background(backgroundColor)
                            .width(cardWidth)
                            .height(cardHeight)
                            .border(5.dp, Color.White.copy(alpha = if (isFocused) 1f else 0.3f))
                            .onFocusChanged { isFocused = it.isFocused }
                            .focusableItem(index)
                    )
                }
            }
        }
    }
}
