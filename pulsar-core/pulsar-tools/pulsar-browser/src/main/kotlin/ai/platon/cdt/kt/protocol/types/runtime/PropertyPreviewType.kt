package ai.platon.cdt.kt.protocol.types.runtime

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Object type. Accessor means that the property itself is an accessor property.
 */
public enum class PropertyPreviewType {
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
  @JsonProperty("accessor")
  ACCESSOR,
  @JsonProperty("bigint")
  BIGINT,
}
