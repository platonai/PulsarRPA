@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.domsnapshot

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.collections.List

data class RareIntegerData(
  @param:JsonProperty("index")
  val index: List<Int>,
  @param:JsonProperty("value")
  val `value`: List<Int>,
)
