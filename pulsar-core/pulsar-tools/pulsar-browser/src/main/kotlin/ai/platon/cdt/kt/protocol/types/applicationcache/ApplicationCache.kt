package ai.platon.cdt.kt.protocol.types.applicationcache

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String
import kotlin.collections.List

/**
 * Detailed application cache information.
 */
public data class ApplicationCache(
  @JsonProperty("manifestURL")
  public val manifestURL: String,
  @JsonProperty("size")
  public val size: Double,
  @JsonProperty("creationTime")
  public val creationTime: Double,
  @JsonProperty("updateTime")
  public val updateTime: Double,
  @JsonProperty("resources")
  public val resources: List<ApplicationCacheResource>,
)
