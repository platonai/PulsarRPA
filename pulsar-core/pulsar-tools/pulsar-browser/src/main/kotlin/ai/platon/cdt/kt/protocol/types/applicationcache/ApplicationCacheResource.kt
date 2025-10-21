package ai.platon.cdt.kt.protocol.types.applicationcache

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Detailed application cache resource information.
 */
public data class ApplicationCacheResource(
  @JsonProperty("url")
  public val url: String,
  @JsonProperty("size")
  public val size: Int,
  @JsonProperty("type")
  public val type: String,
)
