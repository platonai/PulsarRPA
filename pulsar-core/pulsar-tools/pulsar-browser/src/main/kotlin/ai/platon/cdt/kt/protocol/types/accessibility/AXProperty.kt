@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.accessibility

import com.fasterxml.jackson.`annotation`.JsonProperty

data class AXProperty(
  @param:JsonProperty("name")
  val name: AXPropertyName,
  @param:JsonProperty("value")
  val `value`: AXValue,
)
