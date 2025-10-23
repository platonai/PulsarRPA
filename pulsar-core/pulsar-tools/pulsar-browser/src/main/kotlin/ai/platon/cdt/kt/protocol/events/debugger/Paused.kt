@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.debugger

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.debugger.CallFrame
import ai.platon.cdt.kt.protocol.types.debugger.PausedReason
import ai.platon.cdt.kt.protocol.types.runtime.StackTrace
import ai.platon.cdt.kt.protocol.types.runtime.StackTraceId
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Any
import kotlin.Deprecated
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map

/**
 * Fired when the virtual machine stopped on breakpoint or exception or any other stop criteria.
 */
data class Paused(
  @param:JsonProperty("callFrames")
  val callFrames: List<CallFrame>,
  @param:JsonProperty("reason")
  val reason: PausedReason,
  @param:JsonProperty("data")
  @param:Optional
  val `data`: Map<String, Any?>? = null,
  @param:JsonProperty("hitBreakpoints")
  @param:Optional
  val hitBreakpoints: List<String>? = null,
  @param:JsonProperty("asyncStackTrace")
  @param:Optional
  val asyncStackTrace: StackTrace? = null,
  @param:JsonProperty("asyncStackTraceId")
  @param:Optional
  @param:Experimental
  val asyncStackTraceId: StackTraceId? = null,
  @param:JsonProperty("asyncCallStackTraceId")
  @param:Optional
  @Deprecated("Deprecated by protocol")
  @param:Experimental
  val asyncCallStackTraceId: StackTraceId? = null,
)
