@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.fetch

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Response HTTP header entry
 */
data class HeaderEntry(
  @param:JsonProperty("name")
  val name: String,
  @param:JsonProperty("value")
  val `value`: String,
)
