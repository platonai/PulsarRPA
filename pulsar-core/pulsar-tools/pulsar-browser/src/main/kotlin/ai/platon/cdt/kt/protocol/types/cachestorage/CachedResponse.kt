package ai.platon.cdt.kt.protocol.types.cachestorage

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Cached response
 */
public data class CachedResponse(
  @JsonProperty("body")
  public val body: String,
)
