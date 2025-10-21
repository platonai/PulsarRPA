package ai.platon.cdt.kt.protocol.events.webaudio

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Notifies that an AudioNode is connected to an AudioParam.
 */
public data class NodeParamConnected(
  @JsonProperty("contextId")
  public val contextId: String,
  @JsonProperty("sourceId")
  public val sourceId: String,
  @JsonProperty("destinationId")
  public val destinationId: String,
  @JsonProperty("sourceOutputIndex")
  @Optional
  public val sourceOutputIndex: Double? = null,
)
