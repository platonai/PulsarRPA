@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.network

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

/**
 * Determines what type of Trust Token operation is executed and
 * depending on the type, some additional parameters. The values
 * are specified in third_party/blink/renderer/core/fetch/trust_token.idl.
 */
@Experimental
data class TrustTokenParams(
  @param:JsonProperty("type")
  val type: TrustTokenOperationType,
  @param:JsonProperty("refreshPolicy")
  val refreshPolicy: TrustTokenParamsRefreshPolicy,
  @param:JsonProperty("issuers")
  @param:Optional
  val issuers: List<String>? = null,
)
