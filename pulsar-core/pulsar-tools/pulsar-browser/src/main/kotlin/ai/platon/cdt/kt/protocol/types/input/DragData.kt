@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.input

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.collections.List

@Experimental
data class DragData(
  @param:JsonProperty("items")
  val items: List<DragDataItem>,
  @param:JsonProperty("dragOperationsMask")
  val dragOperationsMask: Int,
)
