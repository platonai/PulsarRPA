package ai.platon.cdt.kt.protocol.types.profiler

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Collected counter information.
 */
@Experimental
public data class CounterInfo(
  @JsonProperty("name")
  public val name: String,
  @JsonProperty("value")
  public val `value`: Int,
)
