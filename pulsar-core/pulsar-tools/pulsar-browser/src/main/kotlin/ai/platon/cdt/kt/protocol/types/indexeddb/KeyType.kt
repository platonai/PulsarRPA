package ai.platon.cdt.kt.protocol.types.indexeddb

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Key type.
 */
public enum class KeyType {
  @JsonProperty("number")
  NUMBER,
  @JsonProperty("string")
  STRING,
  @JsonProperty("date")
  DATE,
  @JsonProperty("array")
  ARRAY,
}
