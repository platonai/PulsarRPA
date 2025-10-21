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
public data class LoadingFailed(
  @JsonProperty("requestId")
  public val requestId: String,
  @JsonProperty("timestamp")
  public val timestamp: Double,
  @JsonProperty("type")
  public val type: ResourceType,
  @JsonProperty("errorText")
  public val errorText: String,
  @JsonProperty("canceled")
  @Optional
  public val canceled: Boolean? = null,
  @JsonProperty("blockedReason")
  @Optional
  public val blockedReason: BlockedReason? = null,
  @JsonProperty("corsErrorStatus")
  @Optional
  public val corsErrorStatus: CorsErrorStatus? = null,
)
