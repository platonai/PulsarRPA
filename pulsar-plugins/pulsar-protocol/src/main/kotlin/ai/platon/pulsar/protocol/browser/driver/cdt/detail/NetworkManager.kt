package ai.platon.pulsar.protocol.browser.driver.cdt.detail

import ai.platon.pulsar.common.alwaysFalse
import ai.platon.pulsar.common.event.AbstractEventEmitter
import ai.platon.pulsar.protocol.browser.driver.cdt.ChromeDevtoolsDriver
import ai.platon.pulsar.protocol.browser.driver.cdt.Credentials
import com.github.kklisura.cdt.protocol.v2023.events.fetch.AuthRequired
import com.github.kklisura.cdt.protocol.v2023.events.fetch.RequestPaused
import com.github.kklisura.cdt.protocol.v2023.events.network.*
import com.github.kklisura.cdt.protocol.v2023.types.fetch.AuthChallengeResponse
import com.github.kklisura.cdt.protocol.v2023.types.fetch.AuthChallengeResponseResponse
import com.github.kklisura.cdt.protocol.v2023.types.fetch.RequestPattern
import com.github.kklisura.cdt.protocol.v2023.types.network.Response
import org.slf4j.LoggerFactory
import java.util.*

internal class NetworkManager(
        private val driver: ChromeDevtoolsDriver,
        private val rpc: RobustRPC,
) : AbstractEventEmitter<NetworkManagerEvents>() {
    private val logger = LoggerFactory.getLogger(NetworkManager::class.java)!!
    
    val isActive get() = driver.isActive
    
    private val networkAPI get() = driver.devTools.network.takeIf { isActive }
    private val fetchAPI get() = driver.devTools.fetch.takeIf { isActive }
    private val securityAPI get() = driver.devTools.security.takeIf { isActive }
    
    private val networkEventManager = NetworkEventManager()
    
    // TODO: is it a launch parameter?
    var ignoreHTTPSErrors = true
    val extraHTTPHeaders = mutableMapOf<String, Any>()
    
    var credentials: Credentials? = null
    val attemptedAuthentications = mutableSetOf<String>()
    var userRequestInterceptionEnabled = false
    var protocolRequestInterceptionEnabled = false
    var userCacheDisabled = false
    
    init {
        fetchAPI?.onRequestPaused(::onRequestPaused)
        fetchAPI?.onAuthRequired(::onAuthRequired)
        networkAPI?.onRequestWillBeSent(::onRequestWillBeSent)
        networkAPI?.onRequestWillBeSentExtraInfo(::onRequestWillBeSentExtraInfo)
        networkAPI?.onRequestServedFromCache(::onRequestServedFromCache)
        networkAPI?.onResponseReceived(::onResponseReceived)
        networkAPI?.onLoadingFinished(::onLoadingFinished)
        networkAPI?.onLoadingFailed(::onLoadingFailed)
        networkAPI?.onResponseReceivedExtraInfo(::onResponseReceivedExtraInfo)
        
        rpc.invoke("security") {
            if (ignoreHTTPSErrors) {
                securityAPI?.enable()
                securityAPI?.setIgnoreCertificateErrors(ignoreHTTPSErrors)
            }
        }
        rpc.invoke("enable") {
            networkAPI?.enable()
        }
    }
    
    fun authenticate(credentials: Credentials) {
        this.credentials = credentials
        updateProtocolRequestInterception()
    }
    
    fun setExtraHTTPHeaders(headers: Map<String, String>) {
        extraHTTPHeaders.clear()
        headers.entries.associateTo(extraHTTPHeaders) { it.key.lowercase() to it.value }
        
        rpc.invoke("setExtraHTTPHeaders") {
            networkAPI?.setExtraHTTPHeaders(extraHTTPHeaders)
        }
    }
    
    fun setCacheEnabled(enabled: Boolean) {
        userCacheDisabled = !enabled
        updateProtocolCacheDisabled()
    }
    
    fun setRequestInterception(value: Boolean) {
        userRequestInterceptionEnabled = value
        updateProtocolRequestInterception()
    }
    
    private fun onAuthRequired(event: AuthRequired) {
        logger.info("onAuthRequired | {}", event.requestId)
        
        val response = when {
            attemptedAuthentications.contains(event.requestId) -> AuthChallengeResponseResponse.CANCEL_AUTH
            credentials != null -> AuthChallengeResponseResponse.PROVIDE_CREDENTIALS
            else -> AuthChallengeResponseResponse.DEFAULT
        }
        val authChallengeResponse = AuthChallengeResponse().also {
            it.response = response
            it.username = credentials?.username
            it.password = credentials?.password
        }
        
        rpc.invoke("continueWithAuth") {
            fetchAPI?.continueWithAuth(event.requestId, authChallengeResponse)
        }
    }
    
    private fun onRequestPaused(event: RequestPaused) {
        logger.info("onRequestPaused | {}", event.requestId)
        
        if (!userRequestInterceptionEnabled && protocolRequestInterceptionEnabled) {
            rpc.invoke("continueRequest") {
                fetchAPI?.continueRequest(event.requestId)
            }
        }
        
        val networkRequestId = event.networkId
        val fetchRequestId = event.requestId
        
        if (networkRequestId == null) {
            onRequestWithoutNetworkInstrumentation(event)
            return
        }
        
        val requestWillBeSentEvent = computeRequestWillBeSentEvent(networkRequestId, event)
        if (requestWillBeSentEvent != null) {
            patchRequestEventHeaders(requestWillBeSentEvent, event)
            onRequest(requestWillBeSentEvent, fetchRequestId)
        } else {
            networkEventManager.addRequestPausedEvent(networkRequestId, event)
        }
    }
    
    private fun computeRequestWillBeSentEvent(networkRequestId: String, event: RequestPaused): RequestWillBeSent? {
        val willSentEvent = networkEventManager.getRequestWillBeSentEvent(networkRequestId)
        // redirect requests have the same `requestId`,
        val different = willSentEvent != null &&
                (willSentEvent.request.url != event.request.url || willSentEvent.request.method != event.request.method)
        
        if (different) {
            networkEventManager.removeRequestWillBeSentEvent(networkRequestId)
        }
        
        return if (!different) willSentEvent else null
    }
    
    private fun onRequestWillBeSent(event: RequestWillBeSent) {
        logger.info("onRequestWillBeSent | {}", event.requestId)
        // Request interception doesn't happen for data URLs with Network Service.
        
        val url = event.request.url
        val intercept = userRequestInterceptionEnabled && !url.startsWith("data:")
        if (!intercept) {
            onRequest(event, null)
            return
        }
        
        val networkRequestId = event.requestId
        networkEventManager.addRequestWillBeSentEvent(networkRequestId, event)
        
        /**
         * CDP may have sent a Fetch.requestPaused event already. Check for it.
         */
        val requestPausedEvent = networkEventManager.getRequestPausedEvent(networkRequestId)
        if (requestPausedEvent != null) {
            val fetchRequestId = requestPausedEvent.requestId
            patchRequestEventHeaders(event, requestPausedEvent)
            onRequest(event, fetchRequestId)
            networkEventManager.removeRequestPausedEvent(networkRequestId)
        }
    }
    
    private fun onRequestWillBeSentExtraInfo(event: RequestWillBeSentExtraInfo) {
        networkEventManager.addRequestWillBeSentExtraInfo(event)
    }
    
    private fun onRequest(event: RequestWillBeSent, fetchRequestId: String?) {
        val requestId = event.requestId
        
        logger.info("onRequest | {}", requestId)
        
        var redirectChain: Queue<CDPRequest> = LinkedList()
        if (event.redirectResponse != null) {
            // We want to emit a response and RequestFinished for the
            // redirectResponse, but we can't do so unless we have a
            // responseExtraInfo ready to pair it up with. If we don't have any
            // responseExtraInfos saved in our queue, then we have to wait until
            // the next one to emit response and RequestFinished, *and* we should
            // also wait to emit this Request too because it should come after the
            // response/RequestFinished.
            var redirectResponseExtraInfo: ResponseReceivedExtraInfo? = null
            // TODO: event.redirectHasExtraInfo should exist, we might need upgrade the version of CDP
            // val redirectHasExtraInfo = event.redirectHasExtraInfo
            val redirectHasExtraInfo2 = alwaysFalse()
            if (redirectHasExtraInfo2) {
                // take the first element from the queue and remove it.
                redirectResponseExtraInfo = networkEventManager.takeFirstResponseExtraInfo(requestId)
                if (redirectResponseExtraInfo == null) {
                    networkEventManager.queueRedirectInfo(requestId, RedirectInfo(event, fetchRequestId))
                    return
                }
            }
            
            val request = networkEventManager.getRequest(requestId)
            if (request != null) {
                handleRequestRedirect(request, event.redirectResponse, redirectResponseExtraInfo)
                redirectChain = request.redirectChain
            }
        }
        
        // TODO: add a frame manager
        val frame = event.frameId
        
        require(requestId == event.requestId) { "Inconsistent request id: <${event.requestId}> <- <$requestId>" }
        val request = CDPRequest(driver, requestId, event.request, fetchRequestId, userRequestInterceptionEnabled, redirectChain)
        request.also {
            it.loaderId = event.loaderId
            it.documentURL = event.documentURL
            it.initiator = event.initiator
            it.type = event.type
        }
        networkEventManager.addRequest(requestId, request)
        
        emit(NetworkManagerEvents.Request, request)
        
        request.finalizeInterceptions()
    }
    
    private fun onRequestServedFromCache(event: RequestServedFromCache) {
        logger.info("onRequestServedFromCache | {}", event.requestId)
        val request = networkEventManager.getRequest(event.requestId)
        if (request != null) {
            request.fromMemoryCache = true
        }
        
        emit(NetworkManagerEvents.RequestServedFromCache, request)
    }
    
    private fun onResponseReceived(event: ResponseReceived) {
        val requestId = event.requestId
        
        logger.info("onResponseReceived | {}", requestId)
        var extraInfo: ResponseReceivedExtraInfo? = null
        val request = networkEventManager.getRequest(requestId)
        // TODO: upgrade our CDP
//        val hasExtraInfo = event.hasExtraInfo
        val hasExtraInfo = alwaysFalse()
        if (request != null && !request.fromMemoryCache && hasExtraInfo) {
            extraInfo = networkEventManager.takeFirstResponseExtraInfo(requestId)
            if (extraInfo == null) {
                networkEventManager.addQueuedEventGroup(requestId, QueuedEventGroup(event))
                return
            }
        }
        
        emitResponseEvent(event, extraInfo)
    }
    
    private fun onLoadingFinished(event: LoadingFinished) {
        val requestId = event.requestId
        logger.info("onLoadingFinished | {}", event.requestId)
        
        val queuedEventGroup =networkEventManager.getQueuedEventGroup(requestId)
        if (queuedEventGroup != null) {
            queuedEventGroup.loadingFinishedEvent = event
        } else {
            emitLoadingFinished(event)
        }
    }
    
    private fun onLoadingFailed(event: LoadingFailed) {
        val requestId = event.requestId
        logger.info("onLoadingFailed | {}", event.requestId)
        
        // If the response event for this request is still waiting on a
        // corresponding ExtraInfo event, then wait to emit this event too.
        val queuedEventGroup = networkEventManager.getQueuedEventGroup(requestId)
        if (queuedEventGroup != null) {
            queuedEventGroup.loadingFailedEvent = event
        } else {
            emitLoadingFailed(event)
        }
    }
    
    private fun onResponseReceivedExtraInfo(event: ResponseReceivedExtraInfo) {
        val requestId = event.requestId
        logger.info("onResponseReceivedExtraInfo | {}", event.requestId)
        
        val info: RedirectInfo? = networkEventManager.takeFirstQueuedRedirectInfo(requestId)
        if (info != null) {
            networkEventManager.addResponseExtraInfo(requestId, event)
            onRequest(info.event, info.fetchRequestId)
            return
        }
        
        val queuedEvents = networkEventManager.getQueuedEventGroup(requestId)
        if (queuedEvents != null) {
            networkEventManager.deleteQueuedEventGroup(requestId)
            emitResponseEvent(queuedEvents.responseReceivedEvent, event)
            queuedEvents.loadingFinishedEvent?.let { emitLoadingFinished(it) }
            queuedEvents.loadingFailedEvent?.let { emitLoadingFailed(it) }
            return
        }
        
        networkEventManager.addResponseExtraInfo(requestId, event)
    }
    
    private fun patchRequestEventHeaders(requestWillBeSent: RequestWillBeSent, requestPaused: RequestPaused) {
        // includes extra headers, like: Accept, Origin
        requestWillBeSent.request.headers.putAll(requestPaused.request.headers)
    }
    
    private fun onRequestWithoutNetworkInstrumentation(event: RequestPaused) {
        // If an event has no networkId it should not have any network events. We still want to dispatch it
        // for the interception by the user.
        val frame = event.frameId
        val interceptionId = event.requestId
        val request = CDPRequest(driver, event.requestId, event.request, interceptionId, userRequestInterceptionEnabled)
        request.also {
//            it.loaderId = event.loaderId
//            it.documentURL = event.documentURL
//            it.initiator = event.initiator
//            it.type = event.type
        }
        emit(NetworkManagerEvents.Request, request)
        request.finalizeInterceptions()
    }
    
    private fun emitResponseEvent(event: ResponseReceived, extraInfo: ResponseReceivedExtraInfo?) {
        val requestId = event.requestId
        val request = networkEventManager.getRequest(requestId) ?: return
        val extraInfos = networkEventManager.getResponseExtraInfo(requestId)
        if (extraInfos.isNotEmpty()) {
            logger.warn("Unexpected extraInfo events for request | {}", requestId)
        }
        
        var extraInfo0 = extraInfo
        // Chromium sends wrong extraInfo events for responses served from cache.
        // See https://github.com/puppeteer/puppeteer/issues/9965 and
        // https://crbug.com/1340398.
        if (event.response.fromDiskCache) {
            extraInfo0 = null
        }
        
        val response = CDPResponse(driver, request, event.response, extraInfo0)
        request.response = response
        
        emit(NetworkManagerEvents.Response, response)
    }
    
    private fun handleRequestRedirect(
            request: CDPRequest, responsePayload: Response, extraInfo: ResponseReceivedExtraInfo?
    ) {
        val response = CDPResponse(driver, request, responsePayload, extraInfo)
        request.response = response
        request.redirectChain.add(request)
//        response._resolveBody(
//                new Error('Response body is unavailable for redirect responses')
//        );
        forgetRequest(request, false)
        
        emit(NetworkManagerEvents.Response, response)
        emit(NetworkManagerEvents.RequestFinished, request)
    }
    
    private fun forgetRequest(request: CDPRequest, events: Boolean) {
        val requestId = request.requestId
        val interceptionId = request.interceptionId
        
        networkEventManager.removeRequest(requestId)
        if (interceptionId != null) {
            attemptedAuthentications.remove(interceptionId)
        }
        
        if (events) {
            networkEventManager.forget(requestId)
        }
    }
    
    private fun updateProtocolRequestInterception() {
        val enabled = userRequestInterceptionEnabled || credentials != null
        if (enabled == protocolRequestInterceptionEnabled) {
            return
        }
        protocolRequestInterceptionEnabled = enabled
        
        updateProtocolCacheDisabled()
        
        if (enabled) {
            val pattern = RequestPattern().also { it.urlPattern = "*" }
            fetchAPI?.enable(listOf(pattern), true)
        } else {
            // TODO: there are other scenarios to enable FetchAPI
            // fetchAPI?.disable()
        }
    }
    
    private fun updateProtocolCacheDisabled() {
        rpc.invoke("setCacheDisabled") {
            networkAPI?.setCacheDisabled(this.userCacheDisabled)
        }
    }
    
    private fun emitLoadingFinished(event: LoadingFinished) {
        // For certain requestIds we never receive requestWillBeSent event.
        // @see https://crbug.com/750469
        val request = networkEventManager.getRequest(event.requestId) ?: return

        // Under certain conditions we never get the Network.responseReceived
        // event from protocol. @see https://crbug.com/883475
        request.response?.resolveBody(null)
        
        forgetRequest(request, true);
        emit(NetworkManagerEvents.RequestFinished, request);
    }

    private fun emitLoadingFailed(event: LoadingFailed) {
        val requestId = event.requestId
        val request = networkEventManager.getRequest(requestId) ?: return
        // For certain requestIds we never receive requestWillBeSent event.
        // @see https://crbug.com/750469
        request.failureText = event.errorText

        request.response?.resolveBody(null)
        forgetRequest(request, true)
        emit(NetworkManagerEvents.RequestFailed, request)
    }
}
