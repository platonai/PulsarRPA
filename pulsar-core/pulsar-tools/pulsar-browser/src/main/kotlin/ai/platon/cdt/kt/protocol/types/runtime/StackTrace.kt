@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.runtime

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

/**
 * Call frames for assertions or error messages.
 */
data class StackTrace(
  @param:JsonProperty("description")
  @param:Optional
  val description: String? = null,
  @param:JsonProperty("callFrames")
  val callFrames: List<CallFrame>,
  @param:JsonProperty("parent")
  @param:Optional
  val parent: StackTrace? = null,
  @param:JsonProperty("parentId")
  @param:Optional
  @param:Experimental
  val parentId: StackTraceId? = null,
)
