@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.css

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Data for a simple selector (these are delimited by commas in a selector list).
 */
data class Value(
  @param:JsonProperty("text")
  val text: String,
  @param:JsonProperty("range")
  @param:Optional
  val range: SourceRange? = null,
)
