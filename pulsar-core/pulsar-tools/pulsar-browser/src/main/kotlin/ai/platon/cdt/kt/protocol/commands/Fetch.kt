@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.events.fetch.AuthRequired
import ai.platon.cdt.kt.protocol.events.fetch.RequestPaused
import ai.platon.cdt.kt.protocol.support.annotations.EventName
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.annotations.Returns
import ai.platon.cdt.kt.protocol.support.types.EventHandler
import ai.platon.cdt.kt.protocol.support.types.EventListener
import ai.platon.cdt.kt.protocol.types.fetch.AuthChallengeResponse
import ai.platon.cdt.kt.protocol.types.fetch.HeaderEntry
import ai.platon.cdt.kt.protocol.types.fetch.RequestPattern
import ai.platon.cdt.kt.protocol.types.fetch.ResponseBody
import ai.platon.cdt.kt.protocol.types.network.ErrorReason
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.Unit
import kotlin.collections.List

/**
 * A domain for letting clients substitute browser's network layer with client code.
 */
interface Fetch {
  /**
   * Disables the fetch domain.
   */
  suspend fun disable()

  /**
   * Enables issuing of requestPaused events. A request will be paused until client
   * calls one of failRequest, fulfillRequest or continueRequest/continueWithAuth.
   * @param patterns If specified, only requests matching any of these patterns will produce
   * fetchRequested event and will be paused until clients response. If not set,
   * all requests will be affected.
   * @param handleAuthRequests If true, authRequired events will be issued and requests will be paused
   * expecting a call to continueWithAuth.
   */
  suspend fun enable(@ParamName("patterns") @Optional patterns: List<RequestPattern>? = null, @ParamName("handleAuthRequests") @Optional handleAuthRequests: Boolean? = null)

  suspend fun enable() {
    return enable(null, null)
  }

  /**
   * Causes the request to fail with specified reason.
   * @param requestId An id the client received in requestPaused event.
   * @param errorReason Causes the request to fail with the given reason.
   */
  suspend fun failRequest(@ParamName("requestId") requestId: String, @ParamName("errorReason") errorReason: ErrorReason)

  /**
   * Provides response to the request.
   * @param requestId An id the client received in requestPaused event.
   * @param responseCode An HTTP response code.
   * @param responseHeaders Response headers.
   * @param binaryResponseHeaders Alternative way of specifying response headers as a \0-separated
   * series of name: value pairs. Prefer the above method unless you
   * need to represent some non-UTF8 values that can't be transmitted
   * over the protocol as text. (Encoded as a base64 string when passed over JSON)
   * @param body A response body. (Encoded as a base64 string when passed over JSON)
   * @param responsePhrase A textual representation of responseCode.
   * If absent, a standard phrase matching responseCode is used.
   */
  suspend fun fulfillRequest(
    @ParamName("requestId") requestId: String,
    @ParamName("responseCode") responseCode: Int,
    @ParamName("responseHeaders") @Optional responseHeaders: List<HeaderEntry>? = null,
    @ParamName("binaryResponseHeaders") @Optional binaryResponseHeaders: String? = null,
    @ParamName("body") @Optional body: String? = null,
    @ParamName("responsePhrase") @Optional responsePhrase: String? = null,
  )

  suspend fun fulfillRequest(@ParamName("requestId") requestId: String, @ParamName("responseCode") responseCode: Int) {
    return fulfillRequest(requestId, responseCode, null, null, null, null)
  }

  /**
   * Continues the request, optionally modifying some of its parameters.
   * @param requestId An id the client received in requestPaused event.
   * @param url If set, the request url will be modified in a way that's not observable by page.
   * @param method If set, the request method is overridden.
   * @param postData If set, overrides the post data in the request. (Encoded as a base64 string when passed over JSON)
   * @param headers If set, overrides the request headers.
   */
  suspend fun continueRequest(
    @ParamName("requestId") requestId: String,
    @ParamName("url") @Optional url: String? = null,
    @ParamName("method") @Optional method: String? = null,
    @ParamName("postData") @Optional postData: String? = null,
    @ParamName("headers") @Optional headers: List<HeaderEntry>? = null,
  )

  suspend fun continueRequest(@ParamName("requestId") requestId: String) {
    return continueRequest(requestId, null, null, null, null)
  }

  /**
   * Continues a request supplying authChallengeResponse following authRequired event.
   * @param requestId An id the client received in authRequired event.
   * @param authChallengeResponse Response to  with an authChallenge.
   */
  suspend fun continueWithAuth(@ParamName("requestId") requestId: String, @ParamName("authChallengeResponse") authChallengeResponse: AuthChallengeResponse)

  /**
   * Causes the body of the response to be received from the server and
   * returned as a single string. May only be issued for a request that
   * is paused in the Response stage and is mutually exclusive with
   * takeResponseBodyForInterceptionAsStream. Calling other methods that
   * affect the request or disabling fetch domain before body is received
   * results in an undefined behavior.
   * @param requestId Identifier for the intercepted request to get body for.
   */
  suspend fun getResponseBody(@ParamName("requestId") requestId: String): ResponseBody

  /**
   * Returns a handle to the stream representing the response body.
   * The request must be paused in the HeadersReceived stage.
   * Note that after this command the request can't be continued
   * as is -- client either needs to cancel it or to provide the
   * response body.
   * The stream only supports sequential read, IO.read will fail if the position
   * is specified.
   * This method is mutually exclusive with getResponseBody.
   * Calling other methods that affect the request or disabling fetch
   * domain before body is received results in an undefined behavior.
   * @param requestId
   */
  @Returns("stream")
  suspend fun takeResponseBodyAsStream(@ParamName("requestId") requestId: String): String

  @EventName("requestPaused")
  fun onRequestPaused(eventListener: EventHandler<RequestPaused>): EventListener

  @EventName("requestPaused")
  fun onRequestPaused(eventListener: suspend (RequestPaused) -> Unit): EventListener

  @EventName("authRequired")
  fun onAuthRequired(eventListener: EventHandler<AuthRequired>): EventListener

  @EventName("authRequired")
  fun onAuthRequired(eventListener: suspend (AuthRequired) -> Unit): EventListener
}
