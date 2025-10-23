@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.runtime

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Object type.
 */
public enum class ObjectPreviewType {
  @JsonProperty("object")
  OBJECT,
  @JsonProperty("function")
  FUNCTION,
  @JsonProperty("undefined")
  UNDEFINED,
  @JsonProperty("string")
  STRING,
  @JsonProperty("number")
  NUMBER,
  @JsonProperty("boolean")
  BOOLEAN,
  @JsonProperty("symbol")
  SYMBOL,
  @JsonProperty("bigint")
  BIGINT,
}
