/*
 * Copyright 2020 The Android Open Source Project
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
// @exportToFramework:skipFile()
package androidx.appsearch.app;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.appsearch.platformstorage.PlatformStorage;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SdkSuppress;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Test;

import java.util.concurrent.ExecutorService;

// TODO(b/227356108): move this test to cts test once we un-hide search suggestion API.
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
public class AppSearchSessionPlatformInternalTest extends AppSearchSessionInternalTestBase {
    @Override
    protected ListenableFuture<AppSearchSession> createSearchSessionAsync(@NonNull String dbName) {
        Context context = ApplicationProvider.getApplicationContext();
        return PlatformStorage.createSearchSessionAsync(
                new PlatformStorage.SearchContext.Builder(context, dbName).build());
    }

    @Override
    protected ListenableFuture<AppSearchSession> createSearchSessionAsync(
            @NonNull String dbName, @NonNull ExecutorService executor) {
        Context context = ApplicationProvider.getApplicationContext();
        return PlatformStorage.createSearchSessionAsync(
                new PlatformStorage.SearchContext.Builder(context, dbName)
                        .setWorkerExecutor(executor).build());
    }

    @Override
    @Test
    public void testSearchSuggestion() throws Exception {
        // TODO(b/227356108) enable the test when suggestion is ready in platform.
    }

    @Override
    @Test
    public void testSearchSuggestion_namespaceFilter() throws Exception {
        // TODO(b/227356108) enable the test when suggestion is ready in platform.
    }

    @Override
    @Test
    public void testSearchSuggestion_differentPrefix() throws Exception {
        // TODO(b/227356108) enable the test when suggestion is ready in platform.
    }

    @Override
    @Test
    public void testSearchSuggestion_differentRankingStrategy() throws Exception {
        // TODO(b/227356108) enable the test when suggestion is ready in platform.
    }

    @Override
    @Test
    public void testSearchSuggestion_removeDocument() throws Exception {
        // TODO(b/227356108) enable the test when suggestion is ready in platform.
    }

    @Override
    @Test
    public void testSearchSuggestion_replacementDocument() throws Exception {
        // TODO(b/227356108) enable the test when suggestion is ready in platform.
    }

    @Override
    @Test
    public void testSearchSuggestion_ignoreOperators() throws Exception {
        // TODO(b/227356108) enable the test when suggestion is ready in platform.
    }

    @Override
    @Test
    public void testSearchSuggestion_schemaFilter() throws Exception {
        // TODO(b/227356108) enable the test when suggestion is ready in platform.
    }

    @Override
    @Test
    public void testSearchSuggestion_propertyFilter() throws Exception {
        // TODO(b/227356108) enable the test when suggestion is ready in platform.
    }
}
