@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.network

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.collections.List

/**
 * Information about a signed exchange response.
 */
@Experimental
data class SignedExchangeInfo(
  @param:JsonProperty("outerResponse")
  val outerResponse: Response,
  @param:JsonProperty("header")
  @param:Optional
  val `header`: SignedExchangeHeader? = null,
  @param:JsonProperty("securityDetails")
  @param:Optional
  val securityDetails: SecurityDetails? = null,
  @param:JsonProperty("errors")
  @param:Optional
  val errors: List<SignedExchangeError>? = null,
)
