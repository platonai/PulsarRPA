@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.network

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.network.BlockedReason
import ai.platon.cdt.kt.protocol.types.network.CorsErrorStatus
import ai.platon.cdt.kt.protocol.types.network.ResourceType
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Double
import kotlin.String

/**
 * Fired when HTTP request has failed to load.
 */
data class LoadingFailed(
  @param:JsonProperty("requestId")
  val requestId: String,
  @param:JsonProperty("timestamp")
  val timestamp: Double,
  @param:JsonProperty("type")
  val type: ResourceType,
  @param:JsonProperty("errorText")
  val errorText: String,
  @param:JsonProperty("canceled")
  @param:Optional
  val canceled: Boolean? = null,
  @param:JsonProperty("blockedReason")
  @param:Optional
  val blockedReason: BlockedReason? = null,
  @param:JsonProperty("corsErrorStatus")
  @param:Optional
  val corsErrorStatus: CorsErrorStatus? = null,
)
