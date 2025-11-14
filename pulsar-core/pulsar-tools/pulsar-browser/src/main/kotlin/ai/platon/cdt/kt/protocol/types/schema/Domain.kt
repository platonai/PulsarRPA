@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.schema

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Description of the protocol domain.
 */
data class Domain(
  @param:JsonProperty("name")
  val name: String,
  @param:JsonProperty("version")
  val version: String,
)
