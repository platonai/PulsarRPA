@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.storage

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Pair of issuer origin and number of available (signed, but not used) Trust
 * Tokens from that issuer.
 */
@Experimental
data class TrustTokens(
  @param:JsonProperty("issuerOrigin")
  val issuerOrigin: String,
  @param:JsonProperty("count")
  val count: Double,
)
