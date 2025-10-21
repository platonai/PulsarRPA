package ai.platon.cdt.kt.protocol.events.webaudio

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Notifies that AudioNodes are disconnected. The destination can be null, and it means all the
 * outgoing connections from the source are disconnected.
 */
public data class NodesDisconnected(
  @JsonProperty("contextId")
  public val contextId: String,
  @JsonProperty("sourceId")
  public val sourceId: String,
  @JsonProperty("destinationId")
  public val destinationId: String,
  @JsonProperty("sourceOutputIndex")
  @Optional
  public val sourceOutputIndex: Double? = null,
  @JsonProperty("destinationInputIndex")
  @Optional
  public val destinationInputIndex: Double? = null,
)
