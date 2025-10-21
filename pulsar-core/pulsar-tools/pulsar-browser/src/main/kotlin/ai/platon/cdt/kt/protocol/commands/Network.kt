package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.events.network.DataReceived
import ai.platon.cdt.kt.protocol.events.network.EventSourceMessageReceived
import ai.platon.cdt.kt.protocol.events.network.LoadingFailed
import ai.platon.cdt.kt.protocol.events.network.LoadingFinished
import ai.platon.cdt.kt.protocol.events.network.RequestIntercepted
import ai.platon.cdt.kt.protocol.events.network.RequestServedFromCache
import ai.platon.cdt.kt.protocol.events.network.RequestWillBeSent
import ai.platon.cdt.kt.protocol.events.network.RequestWillBeSentExtraInfo
import ai.platon.cdt.kt.protocol.events.network.ResourceChangedPriority
import ai.platon.cdt.kt.protocol.events.network.ResponseReceived
import ai.platon.cdt.kt.protocol.events.network.ResponseReceivedExtraInfo
import ai.platon.cdt.kt.protocol.events.network.SignedExchangeReceived
import ai.platon.cdt.kt.protocol.events.network.TrustTokenOperationDone
import ai.platon.cdt.kt.protocol.events.network.WebSocketClosed
import ai.platon.cdt.kt.protocol.events.network.WebSocketCreated
import ai.platon.cdt.kt.protocol.events.network.WebSocketFrameError
import ai.platon.cdt.kt.protocol.events.network.WebSocketFrameReceived
import ai.platon.cdt.kt.protocol.events.network.WebSocketFrameSent
import ai.platon.cdt.kt.protocol.events.network.WebSocketHandshakeResponseReceived
import ai.platon.cdt.kt.protocol.events.network.WebSocketWillSendHandshakeRequest
import ai.platon.cdt.kt.protocol.events.network.WebTransportClosed
import ai.platon.cdt.kt.protocol.events.network.WebTransportConnectionEstablished
import ai.platon.cdt.kt.protocol.events.network.WebTransportCreated
import ai.platon.cdt.kt.protocol.support.annotations.EventName
import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.annotations.ReturnTypeParameter
import ai.platon.cdt.kt.protocol.support.annotations.Returns
import ai.platon.cdt.kt.protocol.support.types.EventHandler
import ai.platon.cdt.kt.protocol.support.types.EventListener
import ai.platon.cdt.kt.protocol.types.debugger.SearchMatch
import ai.platon.cdt.kt.protocol.types.network.AuthChallengeResponse
import ai.platon.cdt.kt.protocol.types.network.ConnectionType
import ai.platon.cdt.kt.protocol.types.network.ContentEncoding
import ai.platon.cdt.kt.protocol.types.network.Cookie
import ai.platon.cdt.kt.protocol.types.network.CookieParam
import ai.platon.cdt.kt.protocol.types.network.CookiePriority
import ai.platon.cdt.kt.protocol.types.network.CookieSameSite
import ai.platon.cdt.kt.protocol.types.network.CookieSourceScheme
import ai.platon.cdt.kt.protocol.types.network.ErrorReason
import ai.platon.cdt.kt.protocol.types.network.LoadNetworkResourceOptions
import ai.platon.cdt.kt.protocol.types.network.LoadNetworkResourcePageResult
import ai.platon.cdt.kt.protocol.types.network.RequestPattern
import ai.platon.cdt.kt.protocol.types.network.ResponseBody
import ai.platon.cdt.kt.protocol.types.network.ResponseBodyForInterception
import ai.platon.cdt.kt.protocol.types.network.SecurityIsolationStatus
import java.lang.Deprecated
import kotlin.Any
import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.String
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.Map

/**
 * Network domain allows tracking network activities of the page. It exposes information about http,
 * file, data and other requests and responses, their headers, bodies, timing, etc.
 */
public interface Network {
  /**
   * Sets a list of content encodings that will be accepted. Empty list means no encoding is
   * accepted.
   * @param encodings List of accepted content encodings.
   */
  @Experimental
  public suspend fun setAcceptedEncodings(@ParamName("encodings") encodings: List<ContentEncoding>)

  /**
   * Clears accepted encodings set by setAcceptedEncodings
   */
  @Experimental
  public suspend fun clearAcceptedEncodingsOverride()

  /**
   * Tells whether clearing browser cache is supported.
   */
  @Deprecated
  @Returns("result")
  public suspend fun canClearBrowserCache(): Boolean

  /**
   * Tells whether clearing browser cookies is supported.
   */
  @Deprecated
  @Returns("result")
  public suspend fun canClearBrowserCookies(): Boolean

  /**
   * Tells whether emulation of network conditions is supported.
   */
  @Deprecated
  @Returns("result")
  public suspend fun canEmulateNetworkConditions(): Boolean

  /**
   * Clears browser cache.
   */
  public suspend fun clearBrowserCache()

  /**
   * Clears browser cookies.
   */
  public suspend fun clearBrowserCookies()

  /**
   * Response to Network.requestIntercepted which either modifies the request to continue with any
   * modifications, or blocks it, or completes it with the provided response bytes. If a network
   * fetch occurs as a result which encounters a redirect an additional Network.requestIntercepted
   * event will be sent with the same InterceptionId.
   * Deprecated, use Fetch.continueRequest, Fetch.fulfillRequest and Fetch.failRequest instead.
   * @param interceptionId
   * @param errorReason If set this causes the request to fail with the given reason. Passing
   * `Aborted` for requests
   * marked with `isNavigationRequest` also cancels the navigation. Must not be set in response
   * to an authChallenge.
   * @param rawResponse If set the requests completes using with the provided base64 encoded raw
   * response, including
   * HTTP status line and headers etc... Must not be set in response to an authChallenge. (Encoded
   * as a base64 string when passed over JSON)
   * @param url If set the request url will be modified in a way that's not observable by page. Must
   * not be
   * set in response to an authChallenge.
   * @param method If set this allows the request method to be overridden. Must not be set in
   * response to an
   * authChallenge.
   * @param postData If set this allows postData to be set. Must not be set in response to an
   * authChallenge.
   * @param headers If set this allows the request headers to be changed. Must not be set in
   * response to an
   * authChallenge.
   * @param authChallengeResponse Response to a requestIntercepted with an authChallenge. Must not
   * be set otherwise.
   */
  @Deprecated
  @Experimental
  public suspend fun continueInterceptedRequest(
    @ParamName("interceptionId") interceptionId: String,
    @ParamName("errorReason") @Optional errorReason: ErrorReason?,
    @ParamName("rawResponse") @Optional rawResponse: String?,
    @ParamName("url") @Optional url: String?,
    @ParamName("method") @Optional method: String?,
    @ParamName("postData") @Optional postData: String?,
    @ParamName("headers") @Optional headers: Map<String, Any?>?,
    @ParamName("authChallengeResponse") @Optional authChallengeResponse: AuthChallengeResponse?,
  )

  @Deprecated
  @Experimental
  public suspend fun continueInterceptedRequest(@ParamName("interceptionId")
      interceptionId: String) {
    return continueInterceptedRequest(interceptionId, null, null, null, null, null, null, null)
  }

  /**
   * Deletes browser cookies with matching name and url or domain/path pair.
   * @param name Name of the cookies to remove.
   * @param url If specified, deletes all the cookies with the given name where domain and path
   * match
   * provided URL.
   * @param domain If specified, deletes only cookies with the exact domain.
   * @param path If specified, deletes only cookies with the exact path.
   */
  public suspend fun deleteCookies(
    @ParamName("name") name: String,
    @ParamName("url") @Optional url: String?,
    @ParamName("domain") @Optional domain: String?,
    @ParamName("path") @Optional path: String?,
  )

  public suspend fun deleteCookies(@ParamName("name") name: String) {
    return deleteCookies(name, null, null, null)
  }

  /**
   * Disables network tracking, prevents network events from being sent to the client.
   */
  public suspend fun disable()

  /**
   * Activates emulation of network conditions.
   * @param offline True to emulate internet disconnection.
   * @param latency Minimum latency from request sent to response headers received (ms).
   * @param downloadThroughput Maximal aggregated download throughput (bytes/sec). -1 disables
   * download throttling.
   * @param uploadThroughput Maximal aggregated upload throughput (bytes/sec).  -1 disables upload
   * throttling.
   * @param connectionType Connection type if known.
   */
  public suspend fun emulateNetworkConditions(
    @ParamName("offline") offline: Boolean,
    @ParamName("latency") latency: Double,
    @ParamName("downloadThroughput") downloadThroughput: Double,
    @ParamName("uploadThroughput") uploadThroughput: Double,
    @ParamName("connectionType") @Optional connectionType: ConnectionType?,
  )

  public suspend fun emulateNetworkConditions(
    @ParamName("offline") offline: Boolean,
    @ParamName("latency") latency: Double,
    @ParamName("downloadThroughput") downloadThroughput: Double,
    @ParamName("uploadThroughput") uploadThroughput: Double,
  ) {
    return emulateNetworkConditions(offline, latency, downloadThroughput, uploadThroughput, null)
  }

  /**
   * Enables network tracking, network events will now be delivered to the client.
   * @param maxTotalBufferSize Buffer size in bytes to use when preserving network payloads (XHRs,
   * etc).
   * @param maxResourceBufferSize Per-resource buffer size in bytes to use when preserving network
   * payloads (XHRs, etc).
   * @param maxPostDataSize Longest post body size (in bytes) that would be included in
   * requestWillBeSent notification
   */
  public suspend fun enable(
    @ParamName("maxTotalBufferSize") @Optional @Experimental maxTotalBufferSize: Int?,
    @ParamName("maxResourceBufferSize") @Optional @Experimental maxResourceBufferSize: Int?,
    @ParamName("maxPostDataSize") @Optional maxPostDataSize: Int?,
  )

  public suspend fun enable() {
    return enable(null, null, null)
  }

  /**
   * Returns all browser cookies. Depending on the backend support, will return detailed cookie
   * information in the `cookies` field.
   */
  @Returns("cookies")
  @ReturnTypeParameter(Cookie::class)
  public suspend fun getAllCookies(): List<Cookie>

  /**
   * Returns the DER-encoded certificate.
   * @param origin Origin to get certificate for.
   */
  @Experimental
  @Returns("tableNames")
  @ReturnTypeParameter(String::class)
  public suspend fun getCertificate(@ParamName("origin") origin: String): List<String>

  /**
   * Returns all browser cookies for the current URL. Depending on the backend support, will return
   * detailed cookie information in the `cookies` field.
   * @param urls The list of URLs for which applicable cookies will be fetched.
   * If not specified, it's assumed to be set to the list containing
   * the URLs of the page and all of its subframes.
   */
  @Returns("cookies")
  @ReturnTypeParameter(Cookie::class)
  public suspend fun getCookies(@ParamName("urls") @Optional urls: List<String>?): List<Cookie>

  @Returns("cookies")
  @ReturnTypeParameter(Cookie::class)
  public suspend fun getCookies(): List<Cookie> {
    return getCookies(null)
  }

  /**
   * Returns content served for the given request.
   * @param requestId Identifier of the network request to get content for.
   */
  public suspend fun getResponseBody(@ParamName("requestId") requestId: String): ResponseBody

  /**
   * Returns post data sent with the request. Returns an error when no data was sent with the
   * request.
   * @param requestId Identifier of the network request to get content for.
   */
  @Returns("postData")
  public suspend fun getRequestPostData(@ParamName("requestId") requestId: String): String

  /**
   * Returns content served for the given currently intercepted request.
   * @param interceptionId Identifier for the intercepted request to get body for.
   */
  @Experimental
  public suspend fun getResponseBodyForInterception(@ParamName("interceptionId")
      interceptionId: String): ResponseBodyForInterception

  /**
   * Returns a handle to the stream representing the response body. Note that after this command,
   * the intercepted request can't be continued as is -- you either need to cancel it or to provide
   * the response body. The stream only supports sequential read, IO.read will fail if the position
   * is specified.
   * @param interceptionId
   */
  @Experimental
  @Returns("stream")
  public suspend fun takeResponseBodyForInterceptionAsStream(@ParamName("interceptionId")
      interceptionId: String): String

  /**
   * This method sends a new XMLHttpRequest which is identical to the original one. The following
   * parameters should be identical: method, url, async, request body, extra headers,
   * withCredentials
   * attribute, user, password.
   * @param requestId Identifier of XHR to replay.
   */
  @Experimental
  public suspend fun replayXHR(@ParamName("requestId") requestId: String)

  /**
   * Searches for given string in response content.
   * @param requestId Identifier of the network response to search.
   * @param query String to search for.
   * @param caseSensitive If true, search is case sensitive.
   * @param isRegex If true, treats string parameter as regex.
   */
  @Experimental
  @Returns("result")
  @ReturnTypeParameter(SearchMatch::class)
  public suspend fun searchInResponseBody(
    @ParamName("requestId") requestId: String,
    @ParamName("query") query: String,
    @ParamName("caseSensitive") @Optional caseSensitive: Boolean?,
    @ParamName("isRegex") @Optional isRegex: Boolean?,
  ): List<SearchMatch>

  @Experimental
  @Returns("result")
  @ReturnTypeParameter(SearchMatch::class)
  public suspend fun searchInResponseBody(@ParamName("requestId") requestId: String,
      @ParamName("query") query: String): List<SearchMatch> {
    return searchInResponseBody(requestId, query, null, null)
  }

  /**
   * Blocks URLs from loading.
   * @param urls URL patterns to block. Wildcards ('*') are allowed.
   */
  @Experimental
  public suspend fun setBlockedURLs(@ParamName("urls") urls: List<String>)

  /**
   * Toggles ignoring of service worker for each request.
   * @param bypass Bypass service worker and load from network.
   */
  @Experimental
  public suspend fun setBypassServiceWorker(@ParamName("bypass") bypass: Boolean)

  /**
   * Toggles ignoring cache for each request. If `true`, cache will not be used.
   * @param cacheDisabled Cache disabled state.
   */
  public suspend fun setCacheDisabled(@ParamName("cacheDisabled") cacheDisabled: Boolean)

  /**
   * Sets a cookie with the given cookie data; may overwrite equivalent cookies if they exist.
   * @param name Cookie name.
   * @param value Cookie value.
   * @param url The request-URI to associate with the setting of the cookie. This value can affect
   * the
   * default domain, path, source port, and source scheme values of the created cookie.
   * @param domain Cookie domain.
   * @param path Cookie path.
   * @param secure True if cookie is secure.
   * @param httpOnly True if cookie is http-only.
   * @param sameSite Cookie SameSite type.
   * @param expires Cookie expiration date, session cookie if not set
   * @param priority Cookie Priority type.
   * @param sameParty True if cookie is SameParty.
   * @param sourceScheme Cookie source scheme type.
   * @param sourcePort Cookie source port. Valid values are {-1, [1, 65535]}, -1 indicates an
   * unspecified port.
   * An unspecified port value allows protocol clients to emulate legacy cookie scope for the port.
   * This is a temporary ability and it will be removed in the future.
   */
  @Returns("success")
  public suspend fun setCookie(
    @ParamName("name") name: String,
    @ParamName("value") `value`: String,
    @ParamName("url") @Optional url: String?,
    @ParamName("domain") @Optional domain: String?,
    @ParamName("path") @Optional path: String?,
    @ParamName("secure") @Optional secure: Boolean?,
    @ParamName("httpOnly") @Optional httpOnly: Boolean?,
    @ParamName("sameSite") @Optional sameSite: CookieSameSite?,
    @ParamName("expires") @Optional expires: Double?,
    @ParamName("priority") @Optional @Experimental priority: CookiePriority?,
    @ParamName("sameParty") @Optional @Experimental sameParty: Boolean?,
    @ParamName("sourceScheme") @Optional @Experimental sourceScheme: CookieSourceScheme?,
    @ParamName("sourcePort") @Optional @Experimental sourcePort: Int?,
  ): Boolean

  @Returns("success")
  public suspend fun setCookie(@ParamName("name") name: String, @ParamName("value")
      `value`: String): Boolean {
    return setCookie(name, `value`, null, null, null, null, null, null, null, null, null, null,
        null)
  }

  /**
   * Sets given cookies.
   * @param cookies Cookies to be set.
   */
  public suspend fun setCookies(@ParamName("cookies") cookies: List<CookieParam>)

  /**
   * For testing.
   * @param maxTotalSize Maximum total buffer size.
   * @param maxResourceSize Maximum per-resource size.
   */
  @Experimental
  public suspend fun setDataSizeLimitsForTest(@ParamName("maxTotalSize") maxTotalSize: Int,
      @ParamName("maxResourceSize") maxResourceSize: Int)

  /**
   * Specifies whether to always send extra HTTP headers with the requests from this page.
   * @param headers Map with extra HTTP headers.
   */
  public suspend fun setExtraHTTPHeaders(@ParamName("headers") headers: Map<String, Any?>)

  /**
   * Specifies whether to attach a page script stack id in requests
   * @param enabled Whether to attach a page script stack for debugging purpose.
   */
  @Experimental
  public suspend fun setAttachDebugStack(@ParamName("enabled") enabled: Boolean)

  /**
   * Sets the requests to intercept that match the provided patterns and optionally resource types.
   * Deprecated, please use Fetch.enable instead.
   * @param patterns Requests matching any of these patterns will be forwarded and wait for the
   * corresponding
   * continueInterceptedRequest call.
   */
  @Deprecated
  @Experimental
  public suspend fun setRequestInterception(@ParamName("patterns") patterns: List<RequestPattern>)

  /**
   * Returns information about the COEP/COOP isolation status.
   * @param frameId If no frameId is provided, the status of the target is provided.
   */
  @Experimental
  @Returns("status")
  public suspend fun getSecurityIsolationStatus(@ParamName("frameId") @Optional frameId: String?):
      SecurityIsolationStatus

  @Experimental
  @Returns("status")
  public suspend fun getSecurityIsolationStatus(): SecurityIsolationStatus {
    return getSecurityIsolationStatus(null)
  }

  /**
   * Fetches the resource and returns the content.
   * @param frameId Frame id to get the resource for.
   * @param url URL of the resource to get content for.
   * @param options Options for the request.
   */
  @Experimental
  @Returns("resource")
  public suspend fun loadNetworkResource(
    @ParamName("frameId") frameId: String,
    @ParamName("url") url: String,
    @ParamName("options") options: LoadNetworkResourceOptions,
  ): LoadNetworkResourcePageResult

  @EventName("dataReceived")
  public fun onDataReceived(eventListener: EventHandler<DataReceived>): EventListener

  @EventName("dataReceived")
  public fun onDataReceived(eventListener: suspend (DataReceived) -> Unit): EventListener

  @EventName("eventSourceMessageReceived")
  public fun onEventSourceMessageReceived(eventListener: EventHandler<EventSourceMessageReceived>):
      EventListener

  @EventName("eventSourceMessageReceived")
  public
      fun onEventSourceMessageReceived(eventListener: suspend (EventSourceMessageReceived) -> Unit):
      EventListener

  @EventName("loadingFailed")
  public fun onLoadingFailed(eventListener: EventHandler<LoadingFailed>): EventListener

  @EventName("loadingFailed")
  public fun onLoadingFailed(eventListener: suspend (LoadingFailed) -> Unit): EventListener

  @EventName("loadingFinished")
  public fun onLoadingFinished(eventListener: EventHandler<LoadingFinished>): EventListener

  @EventName("loadingFinished")
  public fun onLoadingFinished(eventListener: suspend (LoadingFinished) -> Unit): EventListener

  @EventName("requestIntercepted")
  @Deprecated
  @Experimental
  public fun onRequestIntercepted(eventListener: EventHandler<RequestIntercepted>): EventListener

  @EventName("requestIntercepted")
  @Deprecated
  @Experimental
  public fun onRequestIntercepted(eventListener: suspend (RequestIntercepted) -> Unit):
      EventListener

  @EventName("requestServedFromCache")
  public fun onRequestServedFromCache(eventListener: EventHandler<RequestServedFromCache>):
      EventListener

  @EventName("requestServedFromCache")
  public fun onRequestServedFromCache(eventListener: suspend (RequestServedFromCache) -> Unit):
      EventListener

  @EventName("requestWillBeSent")
  public fun onRequestWillBeSent(eventListener: EventHandler<RequestWillBeSent>): EventListener

  @EventName("requestWillBeSent")
  public fun onRequestWillBeSent(eventListener: suspend (RequestWillBeSent) -> Unit): EventListener

  @EventName("resourceChangedPriority")
  @Experimental
  public fun onResourceChangedPriority(eventListener: EventHandler<ResourceChangedPriority>):
      EventListener

  @EventName("resourceChangedPriority")
  @Experimental
  public fun onResourceChangedPriority(eventListener: suspend (ResourceChangedPriority) -> Unit):
      EventListener

  @EventName("signedExchangeReceived")
  @Experimental
  public fun onSignedExchangeReceived(eventListener: EventHandler<SignedExchangeReceived>):
      EventListener

  @EventName("signedExchangeReceived")
  @Experimental
  public fun onSignedExchangeReceived(eventListener: suspend (SignedExchangeReceived) -> Unit):
      EventListener

  @EventName("responseReceived")
  public fun onResponseReceived(eventListener: EventHandler<ResponseReceived>): EventListener

  @EventName("responseReceived")
  public fun onResponseReceived(eventListener: suspend (ResponseReceived) -> Unit): EventListener

  @EventName("webSocketClosed")
  public fun onWebSocketClosed(eventListener: EventHandler<WebSocketClosed>): EventListener

  @EventName("webSocketClosed")
  public fun onWebSocketClosed(eventListener: suspend (WebSocketClosed) -> Unit): EventListener

  @EventName("webSocketCreated")
  public fun onWebSocketCreated(eventListener: EventHandler<WebSocketCreated>): EventListener

  @EventName("webSocketCreated")
  public fun onWebSocketCreated(eventListener: suspend (WebSocketCreated) -> Unit): EventListener

  @EventName("webSocketFrameError")
  public fun onWebSocketFrameError(eventListener: EventHandler<WebSocketFrameError>): EventListener

  @EventName("webSocketFrameError")
  public fun onWebSocketFrameError(eventListener: suspend (WebSocketFrameError) -> Unit):
      EventListener

  @EventName("webSocketFrameReceived")
  public fun onWebSocketFrameReceived(eventListener: EventHandler<WebSocketFrameReceived>):
      EventListener

  @EventName("webSocketFrameReceived")
  public fun onWebSocketFrameReceived(eventListener: suspend (WebSocketFrameReceived) -> Unit):
      EventListener

  @EventName("webSocketFrameSent")
  public fun onWebSocketFrameSent(eventListener: EventHandler<WebSocketFrameSent>): EventListener

  @EventName("webSocketFrameSent")
  public fun onWebSocketFrameSent(eventListener: suspend (WebSocketFrameSent) -> Unit):
      EventListener

  @EventName("webSocketHandshakeResponseReceived")
  public
      fun onWebSocketHandshakeResponseReceived(eventListener: EventHandler<WebSocketHandshakeResponseReceived>):
      EventListener

  @EventName("webSocketHandshakeResponseReceived")
  public
      fun onWebSocketHandshakeResponseReceived(eventListener: suspend (WebSocketHandshakeResponseReceived) -> Unit):
      EventListener

  @EventName("webSocketWillSendHandshakeRequest")
  public
      fun onWebSocketWillSendHandshakeRequest(eventListener: EventHandler<WebSocketWillSendHandshakeRequest>):
      EventListener

  @EventName("webSocketWillSendHandshakeRequest")
  public
      fun onWebSocketWillSendHandshakeRequest(eventListener: suspend (WebSocketWillSendHandshakeRequest) -> Unit):
      EventListener

  @EventName("webTransportCreated")
  public fun onWebTransportCreated(eventListener: EventHandler<WebTransportCreated>): EventListener

  @EventName("webTransportCreated")
  public fun onWebTransportCreated(eventListener: suspend (WebTransportCreated) -> Unit):
      EventListener

  @EventName("webTransportConnectionEstablished")
  public
      fun onWebTransportConnectionEstablished(eventListener: EventHandler<WebTransportConnectionEstablished>):
      EventListener

  @EventName("webTransportConnectionEstablished")
  public
      fun onWebTransportConnectionEstablished(eventListener: suspend (WebTransportConnectionEstablished) -> Unit):
      EventListener

  @EventName("webTransportClosed")
  public fun onWebTransportClosed(eventListener: EventHandler<WebTransportClosed>): EventListener

  @EventName("webTransportClosed")
  public fun onWebTransportClosed(eventListener: suspend (WebTransportClosed) -> Unit):
      EventListener

  @EventName("requestWillBeSentExtraInfo")
  @Experimental
  public fun onRequestWillBeSentExtraInfo(eventListener: EventHandler<RequestWillBeSentExtraInfo>):
      EventListener

  @EventName("requestWillBeSentExtraInfo")
  @Experimental
  public
      fun onRequestWillBeSentExtraInfo(eventListener: suspend (RequestWillBeSentExtraInfo) -> Unit):
      EventListener

  @EventName("responseReceivedExtraInfo")
  @Experimental
  public fun onResponseReceivedExtraInfo(eventListener: EventHandler<ResponseReceivedExtraInfo>):
      EventListener

  @EventName("responseReceivedExtraInfo")
  @Experimental
  public
      fun onResponseReceivedExtraInfo(eventListener: suspend (ResponseReceivedExtraInfo) -> Unit):
      EventListener

  @EventName("trustTokenOperationDone")
  @Experimental
  public fun onTrustTokenOperationDone(eventListener: EventHandler<TrustTokenOperationDone>):
      EventListener

  @EventName("trustTokenOperationDone")
  @Experimental
  public fun onTrustTokenOperationDone(eventListener: suspend (TrustTokenOperationDone) -> Unit):
      EventListener
}
