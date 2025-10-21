package ai.platon.cdt.kt.protocol.events.storage

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * The origin's IndexedDB object store has been modified.
 */
public data class IndexedDBContentUpdated(
  @JsonProperty("origin")
  public val origin: String,
  @JsonProperty("databaseName")
  public val databaseName: String,
  @JsonProperty("objectStoreName")
  public val objectStoreName: String,
)
