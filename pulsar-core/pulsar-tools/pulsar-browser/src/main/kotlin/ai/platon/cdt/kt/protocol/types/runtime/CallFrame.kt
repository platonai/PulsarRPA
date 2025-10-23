@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.runtime

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Stack entry for runtime errors and assertions.
 */
data class CallFrame(
  @param:JsonProperty("functionName")
  val functionName: String,
  @param:JsonProperty("scriptId")
  val scriptId: String,
  @param:JsonProperty("url")
  val url: String,
  @param:JsonProperty("lineNumber")
  val lineNumber: Int,
  @param:JsonProperty("columnNumber")
  val columnNumber: Int,
)
