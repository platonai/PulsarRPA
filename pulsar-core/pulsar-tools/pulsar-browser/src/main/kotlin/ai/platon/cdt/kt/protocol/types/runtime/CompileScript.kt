package ai.platon.cdt.kt.protocol.types.runtime

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

public data class CompileScript(
  @JsonProperty("scriptId")
  @Optional
  public val scriptId: String? = null,
  @JsonProperty("exceptionDetails")
  @Optional
  public val exceptionDetails: ExceptionDetails? = null,
)
