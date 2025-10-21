package ai.platon.cdt.kt.protocol.types.systeminfo

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

public data class Info(
  @JsonProperty("gpu")
  public val gpu: GPUInfo,
  @JsonProperty("modelName")
  public val modelName: String,
  @JsonProperty("modelVersion")
  public val modelVersion: String,
  @JsonProperty("commandLine")
  public val commandLine: String,
)
