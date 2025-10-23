@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.console

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Console message.
 */
data class ConsoleMessage(
  @param:JsonProperty("source")
  val source: ConsoleMessageSource,
  @param:JsonProperty("level")
  val level: ConsoleMessageLevel,
  @param:JsonProperty("text")
  val text: String,
  @param:JsonProperty("url")
  @param:Optional
  val url: String? = null,
  @param:JsonProperty("line")
  @param:Optional
  val line: Int? = null,
  @param:JsonProperty("column")
  @param:Optional
  val column: Int? = null,
)
