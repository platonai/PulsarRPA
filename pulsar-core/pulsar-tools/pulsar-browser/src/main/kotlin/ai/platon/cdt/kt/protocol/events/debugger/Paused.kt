package ai.platon.cdt.kt.protocol.events.debugger

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.debugger.CallFrame
import ai.platon.cdt.kt.protocol.types.debugger.PausedReason
import ai.platon.cdt.kt.protocol.types.runtime.StackTrace
import ai.platon.cdt.kt.protocol.types.runtime.StackTraceId
import com.fasterxml.jackson.`annotation`.JsonProperty
import java.lang.Deprecated
import kotlin.Any
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map

/**
 * Fired when the virtual machine stopped on breakpoint or exception or any other stop criteria.
 */
public data class Paused(
  @JsonProperty("callFrames")
  public val callFrames: List<CallFrame>,
  @JsonProperty("reason")
  public val reason: PausedReason,
  @JsonProperty("data")
  @Optional
  public val `data`: Map<String, Any?>? = null,
  @JsonProperty("hitBreakpoints")
  @Optional
  public val hitBreakpoints: List<String>? = null,
  @JsonProperty("asyncStackTrace")
  @Optional
  public val asyncStackTrace: StackTrace? = null,
  @JsonProperty("asyncStackTraceId")
  @Optional
  @Experimental
  public val asyncStackTraceId: StackTraceId? = null,
  @JsonProperty("asyncCallStackTraceId")
  @Optional
  @Deprecated
  @Experimental
  public val asyncCallStackTraceId: StackTraceId? = null,
)
