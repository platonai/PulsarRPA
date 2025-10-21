package ai.platon.cdt.kt.protocol.events.layertree

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.layertree.Layer
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.collections.List

public data class LayerTreeDidChange(
  @JsonProperty("layers")
  @Optional
  public val layers: List<Layer>? = null,
)
