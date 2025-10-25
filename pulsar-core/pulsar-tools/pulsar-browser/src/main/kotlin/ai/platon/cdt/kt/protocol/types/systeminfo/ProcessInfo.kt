@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.systeminfo

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.Int
import kotlin.String

/**
 * Represents process info.
 */
data class ProcessInfo(
  @param:JsonProperty("type")
  val type: String,
  @param:JsonProperty("id")
  val id: Int,
  @param:JsonProperty("cpuTime")
  val cpuTime: Double,
)
