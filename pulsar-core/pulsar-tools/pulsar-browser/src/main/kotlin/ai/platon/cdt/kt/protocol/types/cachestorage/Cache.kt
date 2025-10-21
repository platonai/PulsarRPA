package ai.platon.cdt.kt.protocol.types.cachestorage

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Cache identifier.
 */
public data class Cache(
  @JsonProperty("cacheId")
  public val cacheId: String,
  @JsonProperty("securityOrigin")
  public val securityOrigin: String,
  @JsonProperty("cacheName")
  public val cacheName: String,
)
