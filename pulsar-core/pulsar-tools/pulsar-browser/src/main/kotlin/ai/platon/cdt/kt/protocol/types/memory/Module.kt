@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.memory

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Executable module information
 */
data class Module(
  @param:JsonProperty("name")
  val name: String,
  @param:JsonProperty("uuid")
  val uuid: String,
  @param:JsonProperty("baseAddress")
  val baseAddress: String,
  @param:JsonProperty("size")
  val size: Double,
)
