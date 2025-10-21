package ai.platon.cdt.kt.protocol.types.indexeddb

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String

/**
 * Object store index.
 */
public data class ObjectStoreIndex(
  @JsonProperty("name")
  public val name: String,
  @JsonProperty("keyPath")
  public val keyPath: KeyPath,
  @JsonProperty("unique")
  public val unique: Boolean,
  @JsonProperty("multiEntry")
  public val multiEntry: Boolean,
)
