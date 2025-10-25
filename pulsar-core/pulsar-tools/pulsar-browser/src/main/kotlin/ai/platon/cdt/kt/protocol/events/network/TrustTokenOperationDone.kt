@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.network

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.network.TrustTokenOperationDoneStatus
import ai.platon.cdt.kt.protocol.types.network.TrustTokenOperationType
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Fired exactly once for each Trust Token operation. Depending on
 * the type of the operation and whether the operation succeeded or
 * failed, the event is fired before the corresponding request was sent
 * or after the response was received.
 */
@Experimental
data class TrustTokenOperationDone(
  @param:JsonProperty("status")
  val status: TrustTokenOperationDoneStatus,
  @param:JsonProperty("type")
  val type: TrustTokenOperationType,
  @param:JsonProperty("requestId")
  val requestId: String,
  @param:JsonProperty("topLevelOrigin")
  @param:Optional
  val topLevelOrigin: String? = null,
  @param:JsonProperty("issuerOrigin")
  @param:Optional
  val issuerOrigin: String? = null,
  @param:JsonProperty("issuedTokenCount")
  @param:Optional
  val issuedTokenCount: Int? = null,
)
