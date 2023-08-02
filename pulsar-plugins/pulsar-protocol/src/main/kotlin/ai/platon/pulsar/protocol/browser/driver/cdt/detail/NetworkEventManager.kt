package ai.platon.pulsar.protocol.browser.driver.cdt.detail

import com.github.kklisura.cdt.protocol.v2023.events.fetch.RequestPaused
import com.github.kklisura.cdt.protocol.v2023.events.network.RequestWillBeSent
import com.github.kklisura.cdt.protocol.v2023.events.network.RequestWillBeSentExtraInfo
import com.github.kklisura.cdt.protocol.v2023.events.network.ResponseReceivedExtraInfo
import java.util.*
import java.util.concurrent.ConcurrentHashMap

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
    private val requestWillBeSentMap = ConcurrentHashMap<String, RequestWillBeSent>()
    private val requestWillBeSentExtraInfoMap = ConcurrentHashMap<String, Queue<RequestWillBeSentExtraInfo>>()
    private val requestPausedMap = ConcurrentHashMap<String, RequestPaused>()
    private val CDPRequestsMap = ConcurrentHashMap<String, CDPRequest>()

    /*
     * The below maps are used to reconcile Network.responseReceivedExtraInfo
     * events with their corresponding request. Each response and redirect
     * response gets an ExtraInfo event, and we don't know which will come first.
     * This means that we have to store a Response or an ExtraInfo for each
     * response, and emit the event when we get both of them. In addition, to
     * handle redirects, we have to make them Arrays to represent the chain of
     * events.
     */
    private val responseReceivedExtraInfoMap = ConcurrentHashMap<String, Queue<ResponseReceivedExtraInfo>>()
    private val queuedRedirectInfoMap = ConcurrentHashMap<FetchRequestId, Queue<RedirectInfo>>()
    private val queuedEventGroupMap = ConcurrentHashMap<String, QueuedEventGroup>()

    /**
     * Forget all
     * */
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

    fun takeFirstQueuedRedirectInfo(requestId: FetchRequestId): RedirectInfo? {
        return computeQueuedRedirectInfo(requestId).remove()
    }


    fun addRequestWillBeSentExtraInfo(event: RequestWillBeSentExtraInfo) {

    }


    fun addRequestWillBeSentEvent(networkRequestId: String, event: RequestWillBeSent) {
        requestWillBeSentMap[networkRequestId] = event
    }

    fun getRequestWillBeSentEvent(networkRequestId: String): RequestWillBeSent? {
        return requestWillBeSentMap[networkRequestId]
    }

    fun removeRequestWillBeSentEvent(networkRequestId: String): RequestWillBeSent? {
        return requestWillBeSentMap.remove(networkRequestId)
    }

    fun addRequestPausedEvent(requestId: String, event: RequestPaused) {
        requestPausedMap[requestId] = event
    }

    fun getRequestPausedEvent(requestId: String) = requestPausedMap[requestId]

    fun removeRequestPausedEvent(requestId: String) {
        requestPausedMap.remove(requestId)
    }


    fun addRequest(requestId: String, request: CDPRequest) {
        CDPRequestsMap[requestId] = request
    }

    fun getRequest(requestId: String) = CDPRequestsMap[requestId]

    fun removeRequest(requestId: String) {
        CDPRequestsMap.remove(requestId)
    }


    fun addResponseExtraInfo(requestId: String, event: ResponseReceivedExtraInfo) {
        getResponseExtraInfo(requestId).add(event)
    }

    fun getResponseExtraInfo(requestId: String): Queue<ResponseReceivedExtraInfo> {
        return responseReceivedExtraInfoMap.computeIfAbsent(requestId) { LinkedList() }
    }

    fun deleteResponseExtraInfo(requestId: String) {
        getResponseExtraInfo(requestId).remove()
    }

    fun takeFirstResponseExtraInfo(requestId: String): ResponseReceivedExtraInfo? {
        return getResponseExtraInfo(requestId).remove()
    }


    fun addQueuedEventGroup(requestId: String, event: QueuedEventGroup) {
        queuedEventGroupMap[requestId] = event
    }

    fun getQueuedEventGroup(requestId: String) = queuedEventGroupMap[requestId]

    fun deleteQueuedEventGroup(requestId: String) {
        queuedEventGroupMap.remove(requestId)
    }

    private fun computeQueuedRedirectInfo(requestId: FetchRequestId): Queue<RedirectInfo> {
        return queuedRedirectInfoMap.computeIfAbsent(requestId) { LinkedList() }
    }
}
