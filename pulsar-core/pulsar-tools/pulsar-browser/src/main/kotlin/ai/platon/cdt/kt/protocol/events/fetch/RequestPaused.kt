@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.fetch

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.fetch.HeaderEntry
import ai.platon.cdt.kt.protocol.types.network.ErrorReason
import ai.platon.cdt.kt.protocol.types.network.Request
import ai.platon.cdt.kt.protocol.types.network.ResourceType
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String
import kotlin.collections.List

/**
 * Issued when the domain is enabled and the request URL matches the
 * specified filter. The request is paused until the client responds
 * with one of continueRequest, failRequest or fulfillRequest.
 * The stage of the request can be determined by presence of responseErrorReason
 * and responseStatusCode -- the request is at the response stage if either
 * of these fields is present and in the request stage otherwise.
 */
data class RequestPaused(
  @param:JsonProperty("requestId")
  val requestId: String,
  @param:JsonProperty("request")
  val request: Request,
  @param:JsonProperty("frameId")
  val frameId: String,
  @param:JsonProperty("resourceType")
  val resourceType: ResourceType,
  @param:JsonProperty("responseErrorReason")
  @param:Optional
  val responseErrorReason: ErrorReason? = null,
  @param:JsonProperty("responseStatusCode")
  @param:Optional
  val responseStatusCode: Int? = null,
  @param:JsonProperty("responseHeaders")
  @param:Optional
  val responseHeaders: List<HeaderEntry>? = null,
  @param:JsonProperty("networkId")
  @param:Optional
  val networkId: String? = null,
)
