@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.events.audits.IssueAdded
import ai.platon.cdt.kt.protocol.support.annotations.EventName
import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.types.EventHandler
import ai.platon.cdt.kt.protocol.support.types.EventListener
import ai.platon.cdt.kt.protocol.types.audits.EncodedResponse
import ai.platon.cdt.kt.protocol.types.audits.GetEncodedResponseEncoding
import kotlin.Boolean
import kotlin.Double
import kotlin.String
import kotlin.Unit

/**
 * Audits domain allows investigation of page violations and possible improvements.
 */
@Experimental
interface Audits {
  /**
   * Returns the response body and size if it were re-encoded with the specified settings. Only
   * applies to images.
   * @param requestId Identifier of the network request to get content for.
   * @param encoding The encoding to use.
   * @param quality The quality of the encoding (0-1). (defaults to 1)
   * @param sizeOnly Whether to only return the size information (defaults to false).
   */
  suspend fun getEncodedResponse(
    @ParamName("requestId") requestId: String,
    @ParamName("encoding") encoding: GetEncodedResponseEncoding,
    @ParamName("quality") @Optional quality: Double? = null,
    @ParamName("sizeOnly") @Optional sizeOnly: Boolean? = null,
  ): EncodedResponse

  suspend fun getEncodedResponse(@ParamName("requestId") requestId: String, @ParamName("encoding") encoding: GetEncodedResponseEncoding): EncodedResponse {
    return getEncodedResponse(requestId, encoding, null, null)
  }

  /**
   * Disables issues domain, prevents further issues from being reported to the client.
   */
  suspend fun disable()

  /**
   * Enables issues domain, sends the issues collected so far to the client by means of the
   * `issueAdded` event.
   */
  suspend fun enable()

  /**
   * Runs the contrast check for the target page. Found issues are reported
   * using Audits.issueAdded event.
   * @param reportAAA Whether to report WCAG AAA level issues. Default is false.
   */
  suspend fun checkContrast(@ParamName("reportAAA") @Optional reportAAA: Boolean? = null)

  suspend fun checkContrast() {
    return checkContrast(null)
  }

  @EventName("issueAdded")
  fun onIssueAdded(eventListener: EventHandler<IssueAdded>): EventListener

  @EventName("issueAdded")
  fun onIssueAdded(eventListener: suspend (IssueAdded) -> Unit): EventListener
}
