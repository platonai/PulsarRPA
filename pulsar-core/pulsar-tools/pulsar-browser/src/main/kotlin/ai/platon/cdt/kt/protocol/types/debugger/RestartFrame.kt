@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.debugger

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.runtime.StackTrace
import ai.platon.cdt.kt.protocol.types.runtime.StackTraceId
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.collections.List

data class RestartFrame(
  @param:JsonProperty("callFrames")
  val callFrames: List<CallFrame>,
  @param:JsonProperty("asyncStackTrace")
  @param:Optional
  val asyncStackTrace: StackTrace? = null,
  @param:JsonProperty("asyncStackTraceId")
  @param:Optional
  @param:Experimental
  val asyncStackTraceId: StackTraceId? = null,
)
