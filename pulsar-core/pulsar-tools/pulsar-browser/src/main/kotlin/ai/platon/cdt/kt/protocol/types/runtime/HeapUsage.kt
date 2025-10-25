@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.runtime

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double

data class HeapUsage(
  @param:JsonProperty("usedSize")
  val usedSize: Double,
  @param:JsonProperty("totalSize")
  val totalSize: Double,
)
