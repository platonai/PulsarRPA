package ai.platon.cdt.kt.protocol.events.network

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.network.Initiator
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Fired upon WebSocket creation.
 */
public data class WebSocketCreated(
  @JsonProperty("requestId")
  public val requestId: String,
  @JsonProperty("url")
  public val url: String,
  @JsonProperty("initiator")
  @Optional
  public val initiator: Initiator? = null,
)
