package ai.platon.cdt.kt.protocol.types.indexeddb

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String
import kotlin.collections.List

/**
 * Object store.
 */
public data class ObjectStore(
  @JsonProperty("name")
  public val name: String,
  @JsonProperty("keyPath")
  public val keyPath: KeyPath,
  @JsonProperty("autoIncrement")
  public val autoIncrement: Boolean,
  @JsonProperty("indexes")
  public val indexes: List<ObjectStoreIndex>,
)
