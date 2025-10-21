package ai.platon.cdt.kt.protocol.types.accessibility

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

public data class AXRelatedNode(
  @JsonProperty("backendDOMNodeId")
  public val backendDOMNodeId: Int,
  @JsonProperty("idref")
  @Optional
  public val idref: String? = null,
  @JsonProperty("text")
  @Optional
  public val text: String? = null,
)
