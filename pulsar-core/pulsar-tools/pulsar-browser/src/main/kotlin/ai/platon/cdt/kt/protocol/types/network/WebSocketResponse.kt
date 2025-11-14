@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.network

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Any
import kotlin.Int
import kotlin.String
import kotlin.collections.Map

/**
 * WebSocket response data.
 */
data class WebSocketResponse(
  @param:JsonProperty("status")
  val status: Int,
  @param:JsonProperty("statusText")
  val statusText: String,
  @param:JsonProperty("headers")
  val headers: Map<String, Any?>,
  @param:JsonProperty("headersText")
  @param:Optional
  val headersText: String? = null,
  @param:JsonProperty("requestHeaders")
  @param:Optional
  val requestHeaders: Map<String, Any?>? = null,
  @param:JsonProperty("requestHeadersText")
  @param:Optional
  val requestHeadersText: String? = null,
)
