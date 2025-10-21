package ai.platon.cdt.kt.protocol.types.input

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.collections.List

@Experimental
public data class DragData(
  @JsonProperty("items")
  public val items: List<DragDataItem>,
  @JsonProperty("dragOperationsMask")
  public val dragOperationsMask: Int,
)
