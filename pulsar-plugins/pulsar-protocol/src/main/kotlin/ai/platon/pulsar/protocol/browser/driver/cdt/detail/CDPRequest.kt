package ai.platon.pulsar.protocol.browser.driver.cdt.detail

import ai.platon.pulsar.browser.driver.chrome.util.ChromeRPCException
import ai.platon.pulsar.common.http.HttpStatus
import ai.platon.pulsar.protocol.browser.driver.cdt.ChromeDevtoolsDriver
import com.github.kklisura.cdt.protocol.v2023.types.fetch.HeaderEntry
import com.github.kklisura.cdt.protocol.v2023.types.network.ErrorReason
import com.github.kklisura.cdt.protocol.v2023.types.network.Initiator
import com.github.kklisura.cdt.protocol.v2023.types.network.Request
import com.github.kklisura.cdt.protocol.v2023.types.network.ResourceType
import java.lang.ref.WeakReference
import java.util.*

class CDPRequest(
        val driver: ChromeDevtoolsDriver,
        /**
         * Request identifier.
         */
        var requestId: String,
        /**
         * Request data.
         */
        val request: Request,
        /**
         * Request identifier.
         */
        val interceptionId: String? = null,

        val allowInterception: Boolean = false,

        val redirectChain: Queue<WeakReference<CDPRequest>> = LinkedList()
) {
    /**
     * Loader identifier. Empty string if the request is fetched from worker.
     */
    var loaderId: String? = null
    /**
     * URL of the document this request is loaded for.
     */
    var documentURL: String? = null
    /**
     * Request initiator.
     */
    var initiator: Initiator? = null
    /**
     * Type of this resource.
     */
    var type: ResourceType? = null
    
    var response: CDPResponse? = null
    var fromMemoryCache: Boolean = false

    internal var continueRequestOverrides: ContinueRequestOverrides? = null

    internal var responseForRequest: ResponseForRequest? = null
    
    internal var failureText: String? = null

    var abortErrorReason: ErrorReason? = null

    internal var interceptResolutionState: InterceptResolutionState? = null

    var interceptionHandled = false

    var interceptHandlers = mutableListOf<Any>()

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
            // TODO: check interceptResponse
            val interceptResponse = false
            fetchAPI?.continueRequest(requestId,
                    overrides.url, overrides.method, postDataBinaryBase64, overrides.headers, interceptResponse)
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
