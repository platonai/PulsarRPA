package ai.platon.cdt.kt.protocol.types.indexeddb

import ai.platon.cdt.kt.protocol.types.runtime.RemoteObject
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Data entry.
 */
public data class DataEntry(
  @JsonProperty("key")
  public val key: RemoteObject,
  @JsonProperty("primaryKey")
  public val primaryKey: RemoteObject,
  @JsonProperty("value")
  public val `value`: RemoteObject,
)
