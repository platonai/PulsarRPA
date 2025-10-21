package ai.platon.cdt.kt.protocol.types.indexeddb

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Key path type.
 */
public enum class KeyPathType {
  @JsonProperty("null")
  NULL,
  @JsonProperty("string")
  STRING,
  @JsonProperty("array")
  ARRAY,
}
