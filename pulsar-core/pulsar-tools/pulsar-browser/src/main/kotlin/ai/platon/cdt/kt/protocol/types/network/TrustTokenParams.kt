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
    // vincent, 2025/10/29,
    // com.fasterxml.jackson.databind.exc.ValueInstantiationException: Cannot construct instance of `ai.platon.cdt.kt.protocol.types.network.TrustTokenParams`, problem: Parameter specified as non-null is null: method ai.platon.cdt.kt.protocol.types.network.TrustTokenParams.<init>, parameter type
  @param:JsonProperty("type")
  @param:Optional
  val type: TrustTokenOperationType? = null,
  @param:JsonProperty("refreshPolicy")
  val refreshPolicy: TrustTokenParamsRefreshPolicy,
  @param:JsonProperty("issuers")
  @param:Optional
  val issuers: List<String>? = null,
)
