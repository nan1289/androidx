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

package androidx.credentials

import android.os.Bundle
import androidx.annotation.VisibleForTesting

/**
 * Represents the user's passkey credential granted by the user for app sign-in.
 *
 * @property authenticationResponseJson the public key credential authentication response in
 * JSON format that follows the standard webauthn json format shown at
 * [this w3c link](https://w3c.github.io/webauthn/#dictdef-authenticationresponsejson)
 * @throws NullPointerException If [authenticationResponseJson] is null
 * @throws IllegalArgumentException If [authenticationResponseJson] is empty
 */
class PublicKeyCredential constructor(
    val authenticationResponseJson: String
) : Credential(
    TYPE_PUBLIC_KEY_CREDENTIAL,
    toBundle(authenticationResponseJson)
) {

    init {
        require(authenticationResponseJson.isNotEmpty()) {
            "authentication response JSON must not be empty" }
    }

    /** @hide */
    companion object {
        /** The type value for public key credential related operations. */
        /** @hide */
        const val TYPE_PUBLIC_KEY_CREDENTIAL: String =
            "androidx.credentials.TYPE_PUBLIC_KEY_CREDENTIAL"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val BUNDLE_KEY_AUTHENTICATION_RESPONSE_JSON =
            "androidx.credentials.BUNDLE_KEY_AUTHENTICATION_RESPONSE_JSON"

        @JvmStatic
        internal fun toBundle(authenticationResponseJson: String): Bundle {
            val bundle = Bundle()
            bundle.putString(BUNDLE_KEY_AUTHENTICATION_RESPONSE_JSON, authenticationResponseJson)
            return bundle
        }
    }
}