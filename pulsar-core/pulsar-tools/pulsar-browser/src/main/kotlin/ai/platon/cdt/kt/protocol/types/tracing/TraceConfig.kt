package ai.platon.cdt.kt.protocol.types.tracing

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Any
import kotlin.Boolean
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map

public data class TraceConfig(
  @JsonProperty("recordMode")
  @Optional
  public val recordMode: TraceConfigRecordMode? = null,
  @JsonProperty("enableSampling")
  @Optional
  public val enableSampling: Boolean? = null,
  @JsonProperty("enableSystrace")
  @Optional
  public val enableSystrace: Boolean? = null,
  @JsonProperty("enableArgumentFilter")
  @Optional
  public val enableArgumentFilter: Boolean? = null,
  @JsonProperty("includedCategories")
  @Optional
  public val includedCategories: List<String>? = null,
  @JsonProperty("excludedCategories")
  @Optional
  public val excludedCategories: List<String>? = null,
  @JsonProperty("syntheticDelays")
  @Optional
  public val syntheticDelays: List<String>? = null,
  @JsonProperty("memoryDumpConfig")
  @Optional
  public val memoryDumpConfig: Map<String, Any?>? = null,
)
