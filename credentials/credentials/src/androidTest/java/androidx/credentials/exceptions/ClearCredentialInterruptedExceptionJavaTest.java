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

package androidx.credentials.exceptions;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.google.common.truth.Truth;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ClearCredentialInterruptedExceptionJavaTest {
    @Test(expected = ClearCredentialInterruptedException.class)
    public void construct_inputNonEmpty_success() throws ClearCredentialInterruptedException {
        throw new ClearCredentialInterruptedException("msg");
    }

    @Test(expected = ClearCredentialInterruptedException.class)
    public void construct_errorMessageNull_success() throws ClearCredentialInterruptedException {
        throw new ClearCredentialInterruptedException(null);
    }

    @Test
    public void getter_type_success() {
        ClearCredentialInterruptedException exception = new
                ClearCredentialInterruptedException("msg");
        String expectedType = ClearCredentialInterruptedException
                .TYPE_CLEAR_CREDENTIAL_INTERRUPTED_EXCEPTION;
        Truth.assertThat(exception.getType()).isEqualTo(expectedType);
    }
}
