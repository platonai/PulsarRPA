package ai.platon.cdt.kt.protocol.types.debugger

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.runtime.ExceptionDetails
import ai.platon.cdt.kt.protocol.types.runtime.StackTrace
import ai.platon.cdt.kt.protocol.types.runtime.StackTraceId
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.collections.List

public data class SetScriptSource(
  @JsonProperty("callFrames")
  @Optional
  public val callFrames: List<CallFrame>? = null,
  @JsonProperty("stackChanged")
  @Optional
  public val stackChanged: Boolean? = null,
  @JsonProperty("asyncStackTrace")
  @Optional
  public val asyncStackTrace: StackTrace? = null,
  @JsonProperty("asyncStackTraceId")
  @Optional
  @Experimental
  public val asyncStackTraceId: StackTraceId? = null,
  @JsonProperty("exceptionDetails")
  @Optional
  public val exceptionDetails: ExceptionDetails? = null,
)
