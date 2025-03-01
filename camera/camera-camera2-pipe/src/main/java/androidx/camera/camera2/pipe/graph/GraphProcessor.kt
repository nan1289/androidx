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

@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.graph

import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.GraphState
import androidx.camera.camera2.pipe.GraphState.GraphStateStopped
import androidx.camera.camera2.pipe.GraphState.GraphStateStarting
import androidx.camera.camera2.pipe.GraphState.GraphStateStarted
import androidx.camera.camera2.pipe.CaptureSequenceProcessor
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.config.CameraGraphScope
import androidx.camera.camera2.pipe.config.ForCameraGraph
import androidx.camera.camera2.pipe.core.Debug
import androidx.camera.camera2.pipe.core.Log.debug
import androidx.camera.camera2.pipe.core.Log.warn
import androidx.camera.camera2.pipe.core.Threads
import androidx.camera.camera2.pipe.formatForLogs
import androidx.camera.camera2.pipe.putAllMetadata
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The [GraphProcessor] is responsible for queuing and then submitting them to a
 * [CaptureSequenceProcessor] when it becomes available. This enables interactions to be queued up
 * and submitted before the camera is available.
 */
internal interface GraphProcessor {
    val graphState: StateFlow<GraphState>

    fun submit(request: Request)
    fun submit(requests: List<Request>)
    suspend fun submit(parameters: Map<*, Any?>): Boolean

    fun startRepeating(request: Request)
    fun stopRepeating()

    /**
     * Indicates that internal parameters may have changed, and that the repeating request should
     * be updated as soon as possible.
     */
    fun invalidate()

    /**
     * Abort all submitted requests that have not yet been submitted, as well as asking the
     * [CaptureSequenceProcessor] to abort any submitted requests, which may or may not succeed.
     */
    fun abort()

    /**
     * Closing the [GraphProcessor] will abort all queued requests. Any requests submitted after the
     * [GraphProcessor] is closed will immediately be aborted.
     */
    fun close()
}

/**
 * The graph processor handles *cross-session* state, such as the most recent repeating request.
 */
@CameraGraphScope
internal class GraphProcessorImpl @Inject constructor(
    private val threads: Threads,
    private val cameraGraphConfig: CameraGraph.Config,
    private val graphState3A: GraphState3A,
    @ForCameraGraph private val graphScope: CoroutineScope,
    @ForCameraGraph private val graphListeners: List<@JvmSuppressWildcards Request.Listener>
) : GraphProcessor, GraphListener {
    private val lock = Any()

    @GuardedBy("lock")
    private val submitQueue: MutableList<List<Request>> = ArrayList()

    @GuardedBy("lock")
    private var currentRepeatingRequest: Request? = null

    @GuardedBy("lock")
    private var nextRepeatingRequest: Request? = null

    @GuardedBy("lock")
    private var _requestProcessor: GraphRequestProcessor? = null

    @GuardedBy("lock")
    private var submitting = false

    @GuardedBy("lock")
    private var dirty = false

    @GuardedBy("lock")
    private var closed = false

    private val _graphState = MutableStateFlow<GraphState>(GraphStateStopped)

    override val graphState: StateFlow<GraphState>
        get() = _graphState

    override fun onGraphStarting() {
        debug { "$this onGraphStarting" }
        _graphState.value = GraphStateStarting
    }

    override fun onGraphStarted(requestProcessor: GraphRequestProcessor) {
        debug { "$this onGraphStarted" }
        _graphState.value = GraphStateStarted
        var old: GraphRequestProcessor? = null
        synchronized(lock) {
            if (closed) {
                requestProcessor.close()
                return
            }

            if (_requestProcessor != null && _requestProcessor !== requestProcessor) {
                old = _requestProcessor
            }
            _requestProcessor = requestProcessor
        }

        val processorToClose = old
        if (processorToClose != null) {
            synchronized(processorToClose) {
                processorToClose.close()
            }
        }
        resubmit()
    }

    override fun onGraphStopped(requestProcessor: GraphRequestProcessor) {
        debug { "$this onGraphStopped" }
        _graphState.value = GraphStateStopped
        var old: GraphRequestProcessor? = null
        synchronized(lock) {
            if (closed) {
                return
            }

            if (requestProcessor === _requestProcessor) {
                old = _requestProcessor
                _requestProcessor = null
            } else {
                warn {
                    "Refusing to detach $requestProcessor. " +
                        "It is different from $_requestProcessor"
                }
            }
        }

        val processorToClose = old
        if (processorToClose != null) {
            synchronized(processorToClose) {
                processorToClose.close()
            }
        }
    }

    override fun onGraphModified(requestProcessor: GraphRequestProcessor) {
        synchronized(lock) {
            if (closed) {
                return
            }
            if (requestProcessor !== _requestProcessor) {
                return
            }
        }
        resubmit()
    }

    override fun startRepeating(request: Request) {
        synchronized(lock) {
            if (closed) return
            nextRepeatingRequest = request
            debug { "startRepeating with ${request.formatForLogs()}" }
        }

        graphScope.launch {
            tryStartRepeating()
        }
    }

    override fun stopRepeating() {
        val processor: GraphRequestProcessor?

        synchronized(lock) {
            processor = _requestProcessor
            nextRepeatingRequest = null
            currentRepeatingRequest = null
        }

        graphScope.launch {
            Debug.traceStart { "$this#stopRepeating" }
            // Start with requests that have already been submitted
            if (processor != null) {
                synchronized(processor) {
                    processor.stopRepeating()
                }
            }
            Debug.traceStop()
        }
    }

    override fun submit(request: Request) {
        submit(listOf(request))
    }

    override fun submit(requests: List<Request>) {
        synchronized(lock) {
            if (closed) {
                graphScope.launch(threads.lightweightDispatcher) {
                    abortBurst(requests)
                }
                return
            }
            submitQueue.add(requests)
        }

        graphScope.launch(threads.lightweightDispatcher) {
            submitLoop()
        }
    }

    /**
     * Submit a request to the camera using only the current repeating request.
     */
    override suspend fun submit(parameters: Map<*, Any?>): Boolean =
        withContext(threads.lightweightDispatcher) {
            val processor: GraphRequestProcessor?
            val request: Request?
            val requiredParameters: MutableMap<Any, Any?> = mutableMapOf()

            synchronized(lock) {
                if (closed) return@withContext false
                processor = _requestProcessor
                request = currentRepeatingRequest

                requiredParameters.putAllMetadata(parameters.toMutableMap())
                graphState3A.writeTo(requiredParameters)
                requiredParameters.putAllMetadata(cameraGraphConfig.requiredParameters)
            }

            return@withContext when {
                processor == null || request == null -> false
                else -> processor.submit(
                    isRepeating = false,
                    requests = listOf(request),
                    defaultParameters = cameraGraphConfig.defaultParameters,
                    requiredParameters = requiredParameters,
                    listeners = graphListeners
                )
            }
        }

    override fun invalidate() {
        // Invalidate is only used for updates to internal state (listeners, parameters, etc) and
        // should not (currently) attempt to resubmit the normal request queue.
        graphScope.launch(threads.lightweightDispatcher) {
            tryStartRepeating()
        }
    }

    override fun abort() {
        val processor: GraphRequestProcessor?
        val requests: List<List<Request>>

        synchronized(lock) {
            processor = _requestProcessor
            requests = submitQueue.toList()
            submitQueue.clear()
        }

        graphScope.launch {
            Debug.traceStart { "$this#abort" }
            // Start with requests that have already been submitted
            if (processor != null) {
                synchronized(processor) {
                    processor.abortCaptures()
                }
            }

            // Then abort requests that have not been submitted
            for (burst in requests) {
                abortBurst(burst)
            }
            Debug.traceStop()
        }
    }

    override fun close() {
        val processor: GraphRequestProcessor?
        synchronized(lock) {
            if (closed) {
                return
            }
            closed = true
            processor = _requestProcessor
            _requestProcessor = null
        }

        processor?.close()
        abort()
    }

    private fun resubmit() {
        graphScope.launch(threads.lightweightDispatcher) {
            tryStartRepeating()
            submitLoop()
        }
    }

    private fun abortBurst(requests: List<Request>) {
        for (request in requests) {
            abortRequest(request)
        }
    }

    private fun abortRequest(request: Request) {
        for (listenerIdx in graphListeners.indices) {
            graphListeners[listenerIdx].onAborted(request)
        }

        for (listenerIdx in request.listeners.indices) {
            request.listeners[listenerIdx].onAborted(request)
        }
    }

    private fun tryStartRepeating() {
        val processor: GraphRequestProcessor?
        val request: Request?

        synchronized(lock) {
            if (closed) return

            processor = _requestProcessor
            request = nextRepeatingRequest ?: currentRepeatingRequest

            // TODO: It might be a good idea to turn the "nextRepeatingRequest" into a queue to
            //  help with cases where we want to start the camera early. Example: If we have a
            //  stream configuration where the viewfinder is deferred, but we have an ImageReader
            //  that is _not_ deferred, it may be possible to submit the repeating request.
            //  However, a request with *both* streams would be rejected because not all streams
            //  are ready.
            //  Example:
            //   - Request(listOf(viewfinderStream, otherStream)) // Fails (no viewfinder surface)
            //   - Request(listOf(otherStream)) // works
            //  If (as an app developer) we wanted to make sure the camera starts before the
            //  viewfinder is ready, we would likely want to do something like:
            //   - startRepeating(listOf(otherStream))
            //   - startRepeating(listOf(viewfinderStream, otherStream))
            //  The way this is implemented at the moment, the "nextRepeatingRequest" would be set
            //  to the second call to startRepeating, which would not work. Since the first call got
            //  discarded, we would be unable to start the camera before the viewfinder was
            //  available.
        }

        if (processor != null && request != null) {
            Debug.traceStart { "$this#startRepeating" }
            synchronized(processor) {
                val requiredParameters = mutableMapOf<Any, Any?>()
                graphState3A.writeTo(requiredParameters)
                requiredParameters.putAllMetadata(cameraGraphConfig.requiredParameters)

                if (processor.submit(
                        isRepeating = true,
                        requests = listOf(request),
                        defaultParameters = cameraGraphConfig.defaultParameters,
                        requiredParameters = requiredParameters,
                        listeners = graphListeners
                    )
                ) {
                    // ONLY update the current repeating request if the update succeeds
                    synchronized(lock) {
                        if (processor === _requestProcessor) {
                            currentRepeatingRequest = request

                            // There is a race condition where the nextRepeating request might be changed
                            // while trying to update the current repeating request. If this happens, do no
                            // overwrite the pending request.
                            if (nextRepeatingRequest == request) {
                                nextRepeatingRequest = null
                            }
                        }
                    }
                }
            }
            Debug.traceStop()
        }
    }

    private fun submitLoop() {
        var burst: List<Request>
        var processor: GraphRequestProcessor

        synchronized(lock) {
            if (closed) return

            if (submitting) {
                dirty = true
                return
            }

            val nullableProcessor = _requestProcessor
            val nullableBurst = submitQueue.firstOrNull()
            if (nullableProcessor == null || nullableBurst == null) {
                return
            }

            processor = nullableProcessor
            burst = nullableBurst

            submitting = true
        }

        while (true) {
            var submitted = false
            Debug.traceStart { "$this#submit" }
            try {
                submitted = synchronized(processor) {
                    val requiredParameters = mutableMapOf<Any, Any?>()
                    graphState3A.writeTo(requiredParameters)
                    requiredParameters.putAllMetadata(cameraGraphConfig.requiredParameters)

                    processor.submit(
                        isRepeating = false,
                        requests = burst,
                        defaultParameters = cameraGraphConfig.defaultParameters,
                        requiredParameters = requiredParameters,
                        listeners = graphListeners
                    )
                }
            } finally {
                Debug.traceStop()
                synchronized(lock) {
                    if (submitted) {
                        // submitQueue can potentially be cleared by abort() before entering here.
                        check(submitQueue.isEmpty() || submitQueue.removeAt(0) === burst)

                        val nullableBurst = submitQueue.firstOrNull()
                        if (nullableBurst == null) {
                            dirty = false
                            submitting = false
                            return
                        }

                        burst = nullableBurst
                    } else if (!dirty) {
                        debug { "Failed to submit $burst, and the queue is not dirty." }
                        // If we did not submit, and we are also not dirty, then exit the loop
                        submitting = false
                        return
                    } else {
                        debug {
                            "Failed to submit $burst but the request queue or processor is " +
                                "dirty. Clearing dirty flag and attempting retry."
                        }
                        dirty = false

                        // One possible situation is that the _requestProcessor was replaced or
                        // set to null. If this happens, try to update the requestProcessor we
                        // are currently using. If the current request processor is null, then
                        // we cannot submit anyways.
                        val nullableProcessor = _requestProcessor
                        if (nullableProcessor != null) {
                            processor = nullableProcessor
                        }
                    }
                }
            }
        }
    }
}