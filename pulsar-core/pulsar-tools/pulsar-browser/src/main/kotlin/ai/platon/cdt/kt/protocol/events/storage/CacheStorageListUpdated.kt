package ai.platon.cdt.kt.protocol.events.storage

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * A cache has been added/deleted.
 */
public data class CacheStorageListUpdated(
  @JsonProperty("origin")
  public val origin: String,
)
