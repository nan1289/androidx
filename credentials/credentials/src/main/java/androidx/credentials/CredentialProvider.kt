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

import android.app.Activity
import android.os.CancellationSignal
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import java.util.concurrent.Executor

/**
 * Provider interface to be implemented by system credential providers that
 * will fulfill Credential Manager requests.
 *
 * <p>Note that for SDK version 33 and below, this interface can be implemented by any OEM
 * provider that wishes to return credentials. A provider must :
 * <ol>
 *     <li>Release a dedicated provider library that developers can add as a dependency.
 *     <li>Include an empty CredentialProviderService in the provider library for the purposes of
 *     exposing a meta-data tag in the Android Manifest file.
 *     <li>Add the name of the class that is implementing this interface, as a value
 *     to the meta-data tag described above.
 *     <li>Make sure that there is only one provider implementation on the device. If the device
 *     already has a provider installed, and the developer specifies more than one provider
 *     dependencies, credential manager will error out.
 *</ol>
 *
 * <p>For SDK version 34 and above, this interface will only be implemented by an internal
 * class that will route all requests to the android framework. Providers will need
 * to register directly with the framework to provide credentials.
 *
 * @hide
 */
interface CredentialProvider {
    /**
     * Invoked on a request to get a credential.
     *
     * @param request the request for getting the credential
     * @param activity an optional activity used to potentially launch any UI needed
     * @param cancellationSignal an optional signal that allows for cancelling this call
     * @param executor the callback will take place on this executor
     * @param callback the callback invoked when the request succeeds or fails
     */
    fun onGetCredential(
        request: GetCredentialRequest,
        activity: Activity?, // TODO("Update on optionality")
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<GetCredentialResponse, GetCredentialException>,
    )

    /**
     * Invoked on a request to create a credential.
     *
     * @param request the request for creating the credential
     * @param activity an optional activity used to potentially launch any UI needed
     * @param cancellationSignal an optional signal that allows for cancelling this call
     * @param executor the callback will take place on this executor
     * @param callback the callback invoked when the request succeeds or fails
     */
    fun onCreateCredential(
        request: CreateCredentialRequest,
        activity: Activity?,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<CreateCredentialResponse, CreateCredentialException>,
    )

    /** Determines whether the provider is available on this device, or not. */
    fun isAvailableOnDevice(): Boolean
}