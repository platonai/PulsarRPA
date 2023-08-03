package ai.platon.pulsar.protocol.browser.driver.cdt.detail

import com.github.kklisura.cdt.protocol.v2023.events.network.LoadingFailed
import com.github.kklisura.cdt.protocol.v2023.events.network.LoadingFinished
import com.github.kklisura.cdt.protocol.v2023.events.network.RequestWillBeSent
import com.github.kklisura.cdt.protocol.v2023.events.network.ResponseReceived
import com.github.kklisura.cdt.protocol.v2023.types.fetch.HeaderEntry

enum class NetworkManagerEvents {
    Request, RequestWillBeSent, RequestServedFromCache, Response, ResponseReceived, RequestFailed, RequestFinished
}

enum class InterceptResolutionAction(val action: String) {
    Abort("abort"),
    Respond("respond"),
    Continue("continue"),
    Disabled("disabled"),
    None("none"),
    Handled("handled"),
}

/**
 * The request id for network API.
 * */
typealias NetworkRequestId = String

/**
 * The request id for fetch API.
 * */
typealias FetchRequestId = String

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

class QueuedEventGroup(
        var responseReceivedEvent: ResponseReceived,
        var loadingFinishedEvent: LoadingFinished? = null,
        var loadingFailedEvent: LoadingFailed? = null,
)

class RedirectInfo(
        val event: RequestWillBeSent,
        val fetchRequestId: FetchRequestId? = null
)
