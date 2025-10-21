package ai.platon.cdt.kt.protocol.types.browser

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String
import kotlin.collections.List

/**
 * Chrome histogram.
 */
@Experimental
public data class Histogram(
  @JsonProperty("name")
  public val name: String,
  @JsonProperty("sum")
  public val sum: Int,
  @JsonProperty("count")
  public val count: Int,
  @JsonProperty("buckets")
  public val buckets: List<Bucket>,
)
