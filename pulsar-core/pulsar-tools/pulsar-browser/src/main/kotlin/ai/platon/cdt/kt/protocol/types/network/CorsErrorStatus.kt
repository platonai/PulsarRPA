package ai.platon.cdt.kt.protocol.types.network

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

public data class CorsErrorStatus(
  @JsonProperty("corsError")
  public val corsError: CorsError,
  @JsonProperty("failedParameter")
  public val failedParameter: String,
)
