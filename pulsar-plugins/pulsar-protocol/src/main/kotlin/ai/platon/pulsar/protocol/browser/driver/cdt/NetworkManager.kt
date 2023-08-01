package ai.platon.pulsar.protocol.browser.driver.cdt

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.ChromeTab
import ai.platon.pulsar.browser.driver.chrome.RemoteDevTools
import com.github.kklisura.cdt.protocol.events.fetch.AuthRequired
import com.github.kklisura.cdt.protocol.events.fetch.RequestPaused
import com.github.kklisura.cdt.protocol.events.network.*
import com.github.kklisura.cdt.protocol.types.fetch.RequestPattern
import com.github.kklisura.cdt.protocol.types.network.Request
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

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
    val requestWillBeSentMap = ConcurrentHashMap<String, RequestWillBeSent>()
    val requestPausedMap = ConcurrentHashMap<String, RequestPaused>()
    val httpRequestsMap = ConcurrentHashMap<String, Request>()

    /*
     * The below maps are used to reconcile Network.responseReceivedExtraInfo
     * events with their corresponding request. Each response and redirect
     * response gets an ExtraInfo event, and we don't know which will come first.
     * This means that we have to store a Response or an ExtraInfo for each
     * response, and emit the event when we get both of them. In addition, to
     * handle redirects, we have to make them Arrays to represent the chain of
     * events.
     */
    val responseReceivedExtraInfoMap = ConcurrentHashMap<String, ArrayList<ResponseReceivedExtraInfo>>()
    val queuedRedirectInfoMap = ConcurrentHashMap<String, ArrayList<RedirectInfo>>()
    val queuedEventGroupMap = ConcurrentHashMap<String, ArrayList<QueuedEventGroup>>()

    fun forget(requestId: String) {
        requestWillBeSentMap.remove(requestId);
        requestPausedMap.remove(requestId);
        queuedEventGroupMap.remove(requestId);
        queuedRedirectInfoMap.remove(requestId);
        responseReceivedExtraInfoMap.remove(requestId);
    }

    fun responseExtraInfo(requestId: String): ArrayList<ResponseReceivedExtraInfo> {
        return responseReceivedExtraInfoMap.computeIfAbsent(requestId) { ArrayList() }
    }

    private fun queuedRedirectInfo(requestId: String) {

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
        }

        onRequest(requestWillBeSent, null)
    }
    private fun onRequest(requestWillBeSent: RequestWillBeSent, fetchRequestId: String?) {

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
