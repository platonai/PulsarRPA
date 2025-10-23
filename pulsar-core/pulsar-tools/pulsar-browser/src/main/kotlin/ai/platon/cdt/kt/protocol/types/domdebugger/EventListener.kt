@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.domdebugger

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.runtime.RemoteObject
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Int
import kotlin.String

/**
 * Object event listener.
 */
data class EventListener(
  @param:JsonProperty("type")
  val type: String,
  @param:JsonProperty("useCapture")
  val useCapture: Boolean,
  @param:JsonProperty("passive")
  val passive: Boolean,
  @param:JsonProperty("once")
  val once: Boolean,
  @param:JsonProperty("scriptId")
  val scriptId: String,
  @param:JsonProperty("lineNumber")
  val lineNumber: Int,
  @param:JsonProperty("columnNumber")
  val columnNumber: Int,
  @param:JsonProperty("handler")
  @param:Optional
  val handler: RemoteObject? = null,
  @param:JsonProperty("originalHandler")
  @param:Optional
  val originalHandler: RemoteObject? = null,
  @param:JsonProperty("backendNodeId")
  @param:Optional
  val backendNodeId: Int? = null,
)
