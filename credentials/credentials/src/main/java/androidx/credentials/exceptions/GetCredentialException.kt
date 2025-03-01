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

package androidx.credentials.exceptions

import androidx.annotation.RestrictTo
import androidx.credentials.CredentialManager

/**
 * Represents an error encountered during a get flow with Credential Manager. See
 * [CredentialManager] for more details on how Credentials work for Credential Manager flows.
 *
 * @see CredentialManager
 * @see GetCredentialUnknownException
 * @see GetCredentialCancellationException
 * @see GetCredentialInterruptedException
 *
 * @property errorMessage a human-readable string that describes the error
 * @throws NullPointerException if [type] is null
 * @throws IllegalArgumentException if [type] is empty
 */
open class GetCredentialException @JvmOverloads constructor(
    /** @hide */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val type: String,
    val errorMessage: CharSequence? = null
) : Exception(errorMessage?.toString()) {
    init {
        require(type.isNotEmpty()) { "type must not be empty" }
    }
}