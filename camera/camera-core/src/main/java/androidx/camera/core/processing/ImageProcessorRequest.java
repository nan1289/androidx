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

package androidx.camera.core.processing;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageProcessor;
import androidx.camera.core.ImageProxy;

import java.util.List;

/**
 * Internal implementation of {@link ImageProcessor.Request} for sending {@link ImageProxy} to
 * effect implementations.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ImageProcessorRequest implements ImageProcessor.Request {
    @NonNull
    private final List<ImageProxy> mImageProxies;
    private final int mOutputFormat;

    public ImageProcessorRequest(@NonNull List<ImageProxy> imageProxies, int outputFormat) {
        mImageProxies = imageProxies;
        mOutputFormat = outputFormat;
    }

    @NonNull
    @Override
    public List<ImageProxy> getInputImages() {
        return mImageProxies;
    }

    @Override
    public int getOutputFormat() {
        return mOutputFormat;
    }
}
