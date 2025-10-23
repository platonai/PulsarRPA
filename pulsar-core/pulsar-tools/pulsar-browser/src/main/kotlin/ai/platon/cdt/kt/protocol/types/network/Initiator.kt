@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.network

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.runtime.StackTrace
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Information about the request initiator.
 */
data class Initiator(
  @param:JsonProperty("type")
  val type: InitiatorType,
  @param:JsonProperty("stack")
  @param:Optional
  val stack: StackTrace? = null,
  @param:JsonProperty("url")
  @param:Optional
  val url: String? = null,
  @param:JsonProperty("lineNumber")
  @param:Optional
  val lineNumber: Double? = null,
  @param:JsonProperty("columnNumber")
  @param:Optional
  val columnNumber: Double? = null,
  @param:JsonProperty("requestId")
  @param:Optional
  val requestId: String? = null,
)
