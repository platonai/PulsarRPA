package ai.platon.cdt.kt.protocol.types.debugger

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.runtime.ExceptionDetails
import ai.platon.cdt.kt.protocol.types.runtime.RemoteObject
import com.fasterxml.jackson.`annotation`.JsonProperty

public data class EvaluateOnCallFrame(
  @JsonProperty("result")
  public val result: RemoteObject,
  @JsonProperty("exceptionDetails")
  @Optional
  public val exceptionDetails: ExceptionDetails? = null,
)
