package ai.platon.cdt.kt.protocol.types.network

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Information about the cached resource.
 */
public data class CachedResource(
  @JsonProperty("url")
  public val url: String,
  @JsonProperty("type")
  public val type: ResourceType,
  @JsonProperty("response")
  @Optional
  public val response: Response? = null,
  @JsonProperty("bodySize")
  public val bodySize: Double,
)
