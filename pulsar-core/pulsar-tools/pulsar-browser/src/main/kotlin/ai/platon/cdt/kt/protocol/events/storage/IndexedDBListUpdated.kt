package ai.platon.cdt.kt.protocol.events.storage

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * The origin's IndexedDB database list has been modified.
 */
public data class IndexedDBListUpdated(
  @JsonProperty("origin")
  public val origin: String,
)
