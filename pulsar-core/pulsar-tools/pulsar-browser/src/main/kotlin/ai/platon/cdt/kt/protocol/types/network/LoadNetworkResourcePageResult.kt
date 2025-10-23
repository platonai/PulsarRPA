@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.network

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Any
import kotlin.Boolean
import kotlin.Double
import kotlin.String
import kotlin.collections.Map

/**
 * An object providing the result of a network resource load.
 */
@Experimental
data class LoadNetworkResourcePageResult(
  @param:JsonProperty("success")
  val success: Boolean,
  @param:JsonProperty("netError")
  @param:Optional
  val netError: Double? = null,
  @param:JsonProperty("netErrorName")
  @param:Optional
  val netErrorName: String? = null,
  @param:JsonProperty("httpStatusCode")
  @param:Optional
  val httpStatusCode: Double? = null,
  @param:JsonProperty("stream")
  @param:Optional
  val stream: String? = null,
  @param:JsonProperty("headers")
  @param:Optional
  val headers: Map<String, Any?>? = null,
)
