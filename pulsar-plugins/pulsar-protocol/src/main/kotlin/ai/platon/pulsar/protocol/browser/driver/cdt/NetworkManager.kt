package ai.platon.pulsar.protocol.browser.driver.cdt

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.ChromeTab
import ai.platon.pulsar.browser.driver.chrome.RemoteDevTools
import ai.platon.pulsar.common.alwaysFalse
import com.github.kklisura.cdt.protocol.events.fetch.AuthRequired
import com.github.kklisura.cdt.protocol.events.fetch.RequestPaused
import com.github.kklisura.cdt.protocol.events.network.*
import com.github.kklisura.cdt.protocol.types.fetch.RequestPattern
import com.github.kklisura.cdt.protocol.types.network.Request
import com.github.kklisura.cdt.protocol.types.network.Response
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap

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
    val httpRequestsMap = ConcurrentHashMap<NetworkRequestId, Request>()

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





    fun storeRequest(networkRequestId: NetworkRequestId, event: Request) {
        httpRequestsMap[networkRequestId] = event
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
        val chromeTab: ChromeTab,
        val devTools: RemoteDevTools,
        val browserSettings: BrowserSettings
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
        enableAPIAgents()

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

    suspend fun authenticate(credentials: Credentials?) {
        this.credentials = credentials
        updateProtocolRequestInterception()
    }

    private fun onRequestPaused(requestPaused: RequestPaused) {}

    private fun onAuthRequired(authRequired: AuthRequired) {}
    private fun onRequestWillBeSent(requestWillBeSent: RequestWillBeSent) {
        // Request interception doesn't happen for data URLs with Network Service.

        val url = requestWillBeSent.request.url
        if (userRequestInterceptionEnabled && !url.startsWith("data:")) {
            val requestId = requestWillBeSent.requestId
            networkEventManager.storeRequestWillBeSent(requestId, requestWillBeSent)

            val requestPaused = networkEventManager.getRequestPaused(requestId)
            if (requestPaused != null) {
                patchRequestEventHeaders(requestWillBeSent, requestPaused)
                onRequest(requestWillBeSent, requestId)
                networkEventManager.forgetRequestPaused(requestId)
            }
        } else {
            onRequest(requestWillBeSent, null)
        }
    }

    private fun onRequest(event: RequestWillBeSent, fetchRequestId: String?) {
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

        val request = Request()
        networkEventManager.storeRequest(requestId, request)
        // emit(NetworkManagerEmittedEvents.Request, request);

    }

    private fun handleRequestRedirect(request: Request, redirectResponse: Response?, redirectResponseExtraInfo: ResponseReceivedExtraInfo?) {

    }

    private fun onRequestServedFromCache(requestServedFromCache: RequestServedFromCache) {}

    private fun onResponseReceived(responseReceived: ResponseReceived) {}

    private fun onLoadingFinished(loadingFinished: LoadingFinished) {}

    private fun onLoadingFailed(loadingFailed: LoadingFailed) {}

    private fun onResponseReceivedExtraInfo(responseReceivedExtraInfo: ResponseReceivedExtraInfo) {}

    private fun enableAPIAgents() {
        runtimeAPI?.enable()
        networkAPI?.enable()

//        if (resourceBlockProbability > 1e-6) {
//            fetchAPI?.enable()
//        }

        val proxyUsername = driver.browser.id.fingerprint.proxyUsername
        if (!proxyUsername.isNullOrBlank()) {
            // allow all url patterns
            val patterns = listOf(RequestPattern())
            fetchAPI?.enable(patterns, true)
        }
    }

    private fun patchRequestEventHeaders(requestWillBeSent: RequestWillBeSent, requestPaused: RequestPaused) {
        // includes extra headers, like: Accept, Origin
        requestWillBeSent.request.headers.putAll(requestPaused.request.headers)
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
