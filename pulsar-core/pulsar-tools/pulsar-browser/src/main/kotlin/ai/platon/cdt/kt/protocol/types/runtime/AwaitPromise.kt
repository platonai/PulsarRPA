package ai.platon.cdt.kt.protocol.types.runtime

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty

public data class AwaitPromise(
  @JsonProperty("result")
  public val result: RemoteObject,
  @JsonProperty("exceptionDetails")
  @Optional
  public val exceptionDetails: ExceptionDetails? = null,
)
