package ai.platon.cdt.kt.protocol.types.accessibility

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Any
import kotlin.collections.List

/**
 * A single computed AX property.
 */
public data class AXValue(
  @JsonProperty("type")
  public val type: AXValueType,
  @JsonProperty("value")
  @Optional
  public val `value`: Any? = null,
  @JsonProperty("relatedNodes")
  @Optional
  public val relatedNodes: List<AXRelatedNode>? = null,
  @JsonProperty("sources")
  @Optional
  public val sources: List<AXValueSource>? = null,
)
