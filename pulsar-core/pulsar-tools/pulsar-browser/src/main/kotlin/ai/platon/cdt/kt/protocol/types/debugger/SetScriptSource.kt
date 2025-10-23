@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.debugger

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.runtime.ExceptionDetails
import ai.platon.cdt.kt.protocol.types.runtime.StackTrace
import ai.platon.cdt.kt.protocol.types.runtime.StackTraceId
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.collections.List

data class SetScriptSource(
  @param:JsonProperty("callFrames")
  @param:Optional
  val callFrames: List<CallFrame>? = null,
  @param:JsonProperty("stackChanged")
  @param:Optional
  val stackChanged: Boolean? = null,
  @param:JsonProperty("asyncStackTrace")
  @param:Optional
  val asyncStackTrace: StackTrace? = null,
  @param:JsonProperty("asyncStackTraceId")
  @param:Optional
  @param:Experimental
  val asyncStackTraceId: StackTraceId? = null,
  @param:JsonProperty("exceptionDetails")
  @param:Optional
  val exceptionDetails: ExceptionDetails? = null,
)
