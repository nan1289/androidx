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
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
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
import androidx.tv.material.carousel.Carousel
import androidx.tv.material.carousel.CarouselItem

@Composable
fun FeaturedCarouselContent() {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { SampleLazyRow() }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    repeat(3) {
                        Box(modifier = Modifier
                            .background(Color.Magenta.copy(alpha = 0.3f))
                            .width(50.dp)
                            .height(50.dp)
                            .drawBorderOnFocus()
                        )
                    }
                }
                FeaturedCarousel()
            }
        }
        items(2) { SampleLazyRow() }
    }
}

@Composable
fun Modifier.drawBorderOnFocus(borderColor: Color = Color.White): Modifier {
    var isFocused by remember { mutableStateOf(false) }
    return this
        .border(5.dp, borderColor.copy(alpha = if (isFocused) 1f else 0.2f))
        .onFocusChanged { isFocused = it.isFocused }
        .focusable()
}

@OptIn(ExperimentalTvMaterialApi::class)
@Composable
internal fun FeaturedCarousel() {
    val backgrounds = listOf(
        Color.Red.copy(alpha = 0.3f),
        Color.Yellow.copy(alpha = 0.3f),
        Color.Green.copy(alpha = 0.3f)
    )

    Carousel(
        slideCount = backgrounds.size,
        modifier = Modifier
            .height(300.dp)
            .fillMaxWidth(),
    ) { itemIndex ->
        CarouselItem(
            overlayEnterTransitionStartDelayMillis = 0,
            background = {
                Box(
                    modifier = Modifier
                        .background(backgrounds[itemIndex])
                        .border(2.dp, Color.White.copy(alpha = 0.5f))
                        .fillMaxSize()
                )
            }
        ) {
            Card()
        }
    }
}

@Composable
private fun Card() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        var isFocused by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .border(
                    width = 2.dp,
                    color = if (isFocused) Color.Red else Color.Transparent,
                    shape = RoundedCornerShape(50)
                )
        ) {
            Button(
                onClick = { },
                modifier = Modifier
                    .onFocusChanged { isFocused = it.isFocused }
                    .padding(vertical = 2.dp, horizontal = 5.dp)
            ) {
                Text(text = "Play")
            }
        }
    }
}
