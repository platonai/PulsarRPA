package ai.platon.pulsar.protocol.browser.driver.cdt

import ai.platon.pulsar.browser.driver.chrome.RemoteDevTools
import ai.platon.pulsar.browser.driver.chrome.util.ChromeRPCException
import ai.platon.pulsar.common.alwaysFalse
import ai.platon.pulsar.common.http.HttpStatus
import com.github.kklisura.cdt.protocol.events.fetch.AuthRequired
import com.github.kklisura.cdt.protocol.events.fetch.RequestPaused
import com.github.kklisura.cdt.protocol.events.network.*
import com.github.kklisura.cdt.protocol.types.fetch.HeaderEntry
import com.github.kklisura.cdt.protocol.types.fetch.RequestPattern
import com.github.kklisura.cdt.protocol.types.network.*
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ConcurrentHashMap

enum class InterceptResolutionAction(val action: String) {
    Abort("abort"),
    Respond("respond"),
    Continue("continue"),
    Disabled("disabled"),
    None("none"),
    Handled("handled"),
}

/**
 *
 */
class ContinueRequestOverrides {
    /**
     * If set, the request URL will change. This is not a redirect.
     */
    val url: String? = null
    val method: String? = null
    val postData: String? = null
    val headers: MutableList<HeaderEntry> = mutableListOf()
}

/**
 *
 */
class InterceptResolutionState {
    val action: InterceptResolutionAction? = null
    val priority: Int = 0
}

/**
 * Required response data to fulfill a request with.
 *
 *
 */
class ResponseForRequest {
    val status: Int? = null

    /**
     * Optional response headers. All values are converted to strings.
     */
    val headers: Map<String, String> = mutableMapOf()
    val contentType: String? = null
    val body: Any? = null
}

class HTTPRequest(
        val driver: ChromeDevtoolsDriver,
        /**
         * Request identifier.
         */
        val requestId: String,
        /**
         * Request data.
         */
        val request: Request,
        /**
         * Loader identifier. Empty string if the request is fetched from worker.
         */
        val loaderId: String? = null,
        /**
         * URL of the document this request is loaded for.
         */
        val documentURL: String? = null,
        /**
         * Request initiator.
         */
        val initiator: Initiator? = null,
        /**
         * Type of this resource.
         */
        val type: ResourceType? = null,
) {
    var allowInterception: Boolean = false

    var continueRequestOverrides: ContinueRequestOverrides? = null

    var responseForRequest: ResponseForRequest? = null

    var abortErrorReason: ErrorReason? = null

    var interceptResolutionState: InterceptResolutionState? = null

    var interceptionHandled = false

    var interceptHandlers = mutableListOf<Any>()

    val redirectChain = mutableListOf<HTTPRequest>()

    private var _interceptionId: String? = null

    val isActive get() = driver.isActive

    private val fetchAPI get() = driver.devTools.fetch.takeIf { isActive }

    val url get() = request.url

    fun abortErrorReason(): ErrorReason? {
        require(allowInterception) { "Request Interception is not enabled!" }
        return abortErrorReason
    }

    fun finalizeInterceptions() {
        interceptHandlers.map { }

        val action = interceptResolutionState?.action
        when (action) {
            InterceptResolutionAction.Abort -> abort(abortErrorReason)
            InterceptResolutionAction.Respond -> {
                responseForRequest?.let { respond(it) }
                        ?: throw ChromeRPCException("Response is missing for the interception")
            }

            InterceptResolutionAction.Continue -> {
                continueRequestOverrides?.let { continueRequest(it) }
            }

            else -> {

            }
        }
    }

    private fun continueRequest(overrides: ContinueRequestOverrides) {
        interceptionHandled = true

        val postDataBinaryBase64 = overrides.postData?.let { Base64.getEncoder().encodeToString(it.toByteArray()) }
        val requestId = _interceptionId
                ?: throw ChromeRPCException("HTTPRequest is missing _interceptionId needed for Fetch.continueRequest")

        try {
            fetchAPI?.continueRequest(requestId, overrides.url, overrides.method, postDataBinaryBase64, overrides.headers)
        } catch (e: Exception) {
            interceptionHandled = false
        }
    }

    private fun respond(response: ResponseForRequest) {
        interceptionHandled = true

        val body = responseForRequest?.body
        val responseBody = when {
            body is String -> body
            body is ByteArray -> Base64.getEncoder().encodeToString(body)
            else -> body.toString()
        }

        val responseHeaders = response.headers.entries.mapTo(mutableListOf()) { (name, value) -> headerEntry(name, value) }
        response.contentType?.let { responseHeaders.add(headerEntry("content-type", it)) }
        if (!response.headers.containsKey("content-length")) {
            // TODO: Buffer.byteLength(responseBody)
            responseBody?.let { responseHeaders.add(headerEntry("content-length", it.length.toString())) }
        }
        val binaryResponseHeaders = responseHeaders.joinToString()

        val requestId = _interceptionId
                ?: throw ChromeRPCException("HTTPRequest is missing _interceptionId needed for Fetch.fulfillRequest")

        val responseCode = response.status ?: 200
        val httpStatus = HttpStatus.valueOf(responseCode)

        try {
            fetchAPI?.fulfillRequest(requestId, responseCode, responseHeaders, binaryResponseHeaders, responseBody, httpStatus.reasonPhrase)
        } catch (e: Exception) {
            interceptionHandled = false
        }
    }

    private fun headerEntry(name: String, value: String): HeaderEntry {
        return HeaderEntry().also { it.name = name; it.value = value }
    }

    /**
     * Provides response to the request.
     *
     * @param requestId An id the client received in requestPaused event.
     * @param responseCode An HTTP response code.
     * @param responseHeaders Response headers.
     * @param binaryResponseHeaders Alternative way of specifying response headers as a \0-separated
     * series of name: value pairs. Prefer the above method unless you need to represent some
     * non-UTF8 values that can't be transmitted over the protocol as text. (Encoded as a base64
     * string when passed over JSON)
     * @param body A response body. (Encoded as a base64 string when passed over JSON)
     * @param responsePhrase A textual representation of responseCode. If absent, a standard phrase
     * matching responseCode is used.
     */
//    fun fulfillRequest(
//            @ParamName("requestId") requestId: String?,
//            @ParamName("responseCode") responseCode: Int?,
//            @Optional @ParamName("responseHeaders") responseHeaders: List<HeaderEntry?>?,
//            @Optional @ParamName("binaryResponseHeaders") binaryResponseHeaders: String?,
//            @Optional @ParamName("body") body: String?,
//            @Optional @ParamName("responsePhrase") responsePhrase: String?)

    private fun abort(abortErrorReason: ErrorReason?) {
        interceptionHandled = true

        _interceptionId?.let { fetchAPI?.failRequest(it, abortErrorReason) }
                ?: throw ChromeRPCException("HTTPRequest is missing _interceptionId needed for Fetch.failRequest")
    }

}

typealias NetworkRequestId = String

typealias FetchRequestId = String

class QueuedEventGroup(
        responseReceivedEvent: ResponseReceived,
        loadingFinishedEvent: LoadingFinished? = null,
        loadingFailedEvent: LoadingFailed? = null,
)

class RedirectInfo(
        event: RequestWillBeSent,
        requestId: String? = null
)

/**
 * There are four possible orders of events:
 * A. `_onRequestWillBeSent`
 * B. `_onRequestWillBeSent`, `_onRequestPaused`
 * C. `_onRequestPaused`, `_onRequestWillBeSent`
 * D. `_onRequestPaused`, `_onRequestWillBeSent`, `_onRequestPaused`,
 * `_onRequestWillBeSent`, `_onRequestPaused`, `_onRequestPaused`
 * (see crbug.com/1196004)
 *
 * For `_onRequest` we need the event from `_onRequestWillBeSent` and
 * optionally the `interceptionId` from `_onRequestPaused`.
 *
 * If request interception is disabled, call `_onRequest` once per call to
 * `_onRequestWillBeSent`.
 * If request interception is enabled, call `_onRequest` once per call to
 * `_onRequestPaused` (once per `interceptionId`).
 *
 * Events are stored to allow for subsequent events to call `_onRequest`.
 *
 * Note that (chains of) redirect requests have the same `requestId` (!) as
 * the original request. We have to anticipate series of events like these:
 * A. `_onRequestWillBeSent`,
 * `_onRequestWillBeSent`, ...
 * B. `_onRequestWillBeSent`, `_onRequestPaused`,
 * `_onRequestWillBeSent`, `_onRequestPaused`, ...
 * C. `_onRequestWillBeSent`, `_onRequestPaused`,
 * `_onRequestPaused`, `_onRequestWillBeSent`, ...
 * D. `_onRequestPaused`, `_onRequestWillBeSent`,
 * `_onRequestPaused`, `_onRequestWillBeSent`, `_onRequestPaused`,
 * `_onRequestWillBeSent`, `_onRequestPaused`, `_onRequestPaused`, ...
 * (see crbug.com/1196004)
 */
class NetworkEventManager {
    val requestWillBeSentMap = ConcurrentHashMap<NetworkRequestId, RequestWillBeSent>()
    val requestPausedMap = ConcurrentHashMap<NetworkRequestId, RequestPaused>()
    val httpRequestsMap = ConcurrentHashMap<NetworkRequestId, HTTPRequest>()

    /*
     * The below maps are used to reconcile Network.responseReceivedExtraInfo
     * events with their corresponding request. Each response and redirect
     * response gets an ExtraInfo event, and we don't know which will come first.
     * This means that we have to store a Response or an ExtraInfo for each
     * response, and emit the event when we get both of them. In addition, to
     * handle redirects, we have to make them Arrays to represent the chain of
     * events.
     */
    val responseReceivedExtraInfoMap = ConcurrentHashMap<NetworkRequestId, Queue<ResponseReceivedExtraInfo>>()
    val queuedRedirectInfoMap = ConcurrentHashMap<FetchRequestId, Queue<RedirectInfo>>()
    val queuedEventGroupMap = ConcurrentHashMap<NetworkRequestId, QueuedEventGroup>()

    fun forget(requestId: String) {
        requestWillBeSentMap.remove(requestId);
        requestPausedMap.remove(requestId);
        queuedEventGroupMap.remove(requestId);
        queuedRedirectInfoMap.remove(requestId);
        responseReceivedExtraInfoMap.remove(requestId);
    }

    fun queueRedirectInfo(requestId: FetchRequestId, redirectInfo: RedirectInfo) {
        computeQueuedRedirectInfo(requestId).add(redirectInfo)
    }

    fun takeQueuedRedirectInfo(requestId: FetchRequestId): RedirectInfo? {
        return computeQueuedRedirectInfo(requestId).remove()
    }


    fun storeRequestWillBeSent(networkRequestId: NetworkRequestId, event: RequestWillBeSent) {
        requestWillBeSentMap[networkRequestId] = event
    }

    fun getRequestWillBeSent(networkRequestId: NetworkRequestId): RequestWillBeSent? {
        return requestWillBeSentMap[networkRequestId]
    }

    fun forgetRequestWillBeSent(networkRequestId: NetworkRequestId) {
        requestWillBeSentMap.remove(networkRequestId)
    }

    fun storeRequestPaused(networkRequestId: NetworkRequestId, event: RequestPaused) {
        requestPausedMap[networkRequestId] = event
    }

    fun getRequestPaused(networkRequestId: NetworkRequestId) = requestPausedMap[networkRequestId]

    fun forgetRequestPaused(networkRequestId: NetworkRequestId) {
        requestPausedMap.remove(networkRequestId)
    }


    fun storeRequest(networkRequestId: NetworkRequestId, request: HTTPRequest) {
        httpRequestsMap[networkRequestId] = request
    }

    fun getRequest(networkRequestId: NetworkRequestId) = httpRequestsMap[networkRequestId]

    fun forgetRequest(networkRequestId: NetworkRequestId) {
        httpRequestsMap.remove(networkRequestId)
    }


    fun responseExtraInfo(requestId: String): Queue<ResponseReceivedExtraInfo> {
        return responseReceivedExtraInfoMap.computeIfAbsent(requestId) { LinkedList() }
    }

    fun storeResponseExtraInfo(networkRequestId: NetworkRequestId, event: QueuedEventGroup) {
        queuedEventGroupMap[networkRequestId] = event
    }

    fun getResponseExtraInfo(networkRequestId: NetworkRequestId) = queuedEventGroupMap[networkRequestId]

    fun forgetResponseExtraInfo(networkRequestId: NetworkRequestId) {
        responseExtraInfo(networkRequestId).remove()
    }


    fun storeQueuedEventGroup(networkRequestId: NetworkRequestId, event: QueuedEventGroup) {
        queuedEventGroupMap[networkRequestId] = event
    }

    fun getQueuedEventGroup(networkRequestId: NetworkRequestId) = queuedEventGroupMap[networkRequestId]

    fun forgetQueuedEventGroup(networkRequestId: NetworkRequestId) {
        queuedEventGroupMap.remove(networkRequestId)
    }


    private fun computeQueuedRedirectInfo(requestId: FetchRequestId): Queue<RedirectInfo> {
        return queuedRedirectInfoMap.computeIfAbsent(requestId) { LinkedList() }
    }
}

class NetworkManager(
        val driver: ChromeDevtoolsDriver,
        val devTools: RemoteDevTools,
) {
    private val logger = LoggerFactory.getLogger(NetworkManager::class.java)!!

    val isActive get() = driver.isActive

    private val networkAPI get() = devTools.network.takeIf { isActive }
    private val fetchAPI get() = devTools.fetch.takeIf { isActive }
    private val runtimeAPI get() = devTools.runtime.takeIf { isActive }

    private val networkEventManager = NetworkEventManager()

    var ignoreHTTPSErrors = true
    val extraHTTPHeaders = mutableMapOf<String, String>()

    var credentials: Credentials? = null
    val attemptedAuthentications = mutableSetOf<String>()
    var userRequestInterceptionEnabled = false
    var protocolRequestInterceptionEnabled = false
    var userCacheDisabled = false

    init {
        fetchAPI?.onRequestPaused { requestPaused ->
            onRequestPaused(requestPaused)
        }

        fetchAPI?.onAuthRequired { authRequired ->
            onAuthRequired(authRequired)
        }

        networkAPI?.onRequestWillBeSent { requestWillBeSent ->
            onRequestWillBeSent(requestWillBeSent)
        }

        networkAPI?.onRequestServedFromCache { requestServedFromCache ->
            onRequestServedFromCache(requestServedFromCache)
        }

        networkAPI?.onResponseReceived { responseReceived ->
            onResponseReceived(responseReceived)
        }

        networkAPI?.onLoadingFinished { loadingFinished ->
            onLoadingFinished(loadingFinished)
        }

        networkAPI?.onLoadingFailed { loadingFailed ->
            onLoadingFailed(loadingFailed)
        }

        networkAPI?.onResponseReceivedExtraInfo { responseReceivedExtraInfo ->
            onResponseReceivedExtraInfo(responseReceivedExtraInfo)
        }
    }

    fun authenticate(credentials: Credentials?) {
        this.credentials = credentials
        updateProtocolRequestInterception()
    }

    private fun onRequestPaused(event: RequestPaused) {
        logger.info("onRequestPaused | {}", event.requestId)
        if (!userRequestInterceptionEnabled && protocolRequestInterceptionEnabled) {
            fetchAPI?.continueRequest(event.requestId)
        }

        val networkRequestId = event.networkId
        val fetchRequestId = event.requestId

        if (networkRequestId == null) {
            onRequestWithoutNetworkInstrumentation(event)
            return
        }
    }

    private fun onAuthRequired(event: AuthRequired) {
        logger.info("onAuthRequired | {}", event.requestId)
    }

    private fun onRequestWillBeSent(event: RequestWillBeSent) {
        logger.info("onRequestWillBeSent | {}", event.requestId)
        // Request interception doesn't happen for data URLs with Network Service.

        val url = event.request.url
        if (userRequestInterceptionEnabled && !url.startsWith("data:")) {
            val requestId = event.requestId
            networkEventManager.storeRequestWillBeSent(requestId, event)

            val requestPaused = networkEventManager.getRequestPaused(requestId)
            if (requestPaused != null) {
                patchRequestEventHeaders(event, requestPaused)
                onRequest(event, requestId)
                networkEventManager.forgetRequestPaused(requestId)
            }
        } else {
            onRequest(event, null)
        }
    }

    private fun onRequest(event: RequestWillBeSent, fetchRequestId: String?) {
        logger.info("onRequest | {}", event.requestId)

        val requestId = event.requestId
        val redirectChain = mutableListOf<Request>()
        if (event.redirectResponse != null) {
            var redirectResponseExtraInfo: ResponseReceivedExtraInfo? = null
            val redirectHasExtraInfo = alwaysFalse()
            if (redirectHasExtraInfo) {
                // removes the first element
                val extraInfo = networkEventManager.responseExtraInfo(requestId).remove()
                redirectResponseExtraInfo = extraInfo
                if (extraInfo != null) {
                    networkEventManager.queueRedirectInfo(extraInfo.requestId, RedirectInfo(event, fetchRequestId))
                    return
                }
            }

            val request = networkEventManager.getRequest(event.requestId)
            if (request != null) {
                handleRequestRedirect(request, event.redirectResponse, redirectResponseExtraInfo)
                // redirectChain = request._redirectChain
            }
        }

        val request = HTTPRequest(driver, requestId, event.request)
        networkEventManager.storeRequest(requestId, request)
        // emit(NetworkManagerEmittedEvents.Request, request);
    }

    private fun handleRequestRedirect(request: HTTPRequest, redirectResponse: Response?, redirectResponseExtraInfo: ResponseReceivedExtraInfo?) {
    }

    private fun onRequestServedFromCache(event: RequestServedFromCache) {
        logger.info("onRequestServedFromCache | {}", event.requestId)
    }

    private fun onResponseReceived(event: ResponseReceived) {
        logger.info("onResponseReceived | {}", event.requestId)
    }

    private fun onLoadingFinished(event: LoadingFinished) {
        logger.info("onLoadingFinished | {}", event.requestId)
    }

    private fun onLoadingFailed(event: LoadingFailed) {
        logger.info("onLoadingFailed | {}", event.requestId)
    }

    private fun onResponseReceivedExtraInfo(event: ResponseReceivedExtraInfo) {
        logger.info("onResponseReceivedExtraInfo | {}", event.requestId)
    }

    private fun patchRequestEventHeaders(requestWillBeSent: RequestWillBeSent, requestPaused: RequestPaused) {
        // includes extra headers, like: Accept, Origin
        requestWillBeSent.request.headers.putAll(requestPaused.request.headers)
    }

    private fun onRequestWithoutNetworkInstrumentation(event: RequestPaused) {
        val frame = event.frameId
    }

    private fun updateProtocolRequestInterception() {
        if (userRequestInterceptionEnabled) {
            return
        }

        val enabled = credentials != null
        protocolRequestInterceptionEnabled = enabled

        updateProtocolCacheDisabled()

        if (enabled) {
            val pattern = RequestPattern().also { it.urlPattern = "*" }
            fetchAPI?.enable(listOf(pattern), true)
        } else {
            fetchAPI?.disable()
        }
    }

    private fun updateProtocolCacheDisabled() {
        networkAPI?.setCacheDisabled(this.userCacheDisabled)
    }
}
