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

package androidx.camera.camera2.internal.compat;

import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * Helper for accessing features in {@link StreamConfigurationMap} in a backwards compatible
 * fashion.
 */
@RequiresApi(21)
public class StreamConfigurationMapCompat {

    private final StreamConfigurationMapCompatImpl mImpl;

    private StreamConfigurationMapCompat(@NonNull StreamConfigurationMap map) {
        if (Build.VERSION.SDK_INT >= 23) {
            mImpl = new StreamConfigurationMapCompatApi23Impl(map);
        } else {
            mImpl = new StreamConfigurationMapCompatBaseImpl(map);
        }
    }

    /**
     * Provides a backward-compatible wrapper for {@link StreamConfigurationMap}.
     *
     * @param map {@link StreamConfigurationMap} class to wrap
     * @return wrapped class
     */
    @NonNull
    public static StreamConfigurationMapCompat toStreamConfigurationMapCompat(
            @NonNull StreamConfigurationMap map) {
        return new StreamConfigurationMapCompat(map);
    }

    /**
     * Get a list of sizes compatible with the requested image {@code format}.
     *
     * @param format an image format from {@link ImageFormat} or {@link PixelFormat}
     * @return an array of supported sizes, or {@code null} if the {@code format} is not a
     *         supported output
     *
     * @see ImageFormat
     * @see PixelFormat
     */
    @Nullable
    public Size[] getOutputSizes(int format) {
        return mImpl.getOutputSizes(format);
    }

    /**
     * Get a list of sizes compatible with {@code klass} to use as an output.
     *
     * @param klass a non-{@code null} {@link Class} object reference
     * @return an array of supported sizes for {@link ImageFormat#PRIVATE} format,
     *         or {@code null} iff the {@code klass} is not a supported output.
     *
     * @throws NullPointerException if {@code klass} was {@code null}
     */
    @Nullable
    public <T> Size[] getOutputSizes(@NonNull Class<T> klass) {
        return mImpl.getOutputSizes(klass);
    }

    interface StreamConfigurationMapCompatImpl {

        @Nullable
        Size[] getOutputSizes(int format);

        @Nullable
        <T> Size[] getOutputSizes(@NonNull Class<T> klass);
    }
}
