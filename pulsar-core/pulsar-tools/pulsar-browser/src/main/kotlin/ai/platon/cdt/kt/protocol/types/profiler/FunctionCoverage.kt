package ai.platon.cdt.kt.protocol.types.profiler

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String
import kotlin.collections.List

/**
 * Coverage data for a JavaScript function.
 */
public data class FunctionCoverage(
  @JsonProperty("functionName")
  public val functionName: String,
  @JsonProperty("ranges")
  public val ranges: List<CoverageRange>,
  @JsonProperty("isBlockCoverage")
  public val isBlockCoverage: Boolean,
)
