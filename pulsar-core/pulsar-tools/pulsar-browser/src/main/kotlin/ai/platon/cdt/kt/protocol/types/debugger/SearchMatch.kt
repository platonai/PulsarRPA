@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.debugger

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Search match for resource.
 */
data class SearchMatch(
  @param:JsonProperty("lineNumber")
  val lineNumber: Double,
  @param:JsonProperty("lineContent")
  val lineContent: String,
)
