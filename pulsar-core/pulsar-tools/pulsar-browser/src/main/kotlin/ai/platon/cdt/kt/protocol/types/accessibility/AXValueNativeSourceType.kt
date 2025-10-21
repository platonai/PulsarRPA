package ai.platon.cdt.kt.protocol.types.accessibility

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Enum of possible native property sources (as a subtype of a particular AXValueSourceType).
 */
public enum class AXValueNativeSourceType {
  @JsonProperty("figcaption")
  FIGCAPTION,
  @JsonProperty("label")
  LABEL,
  @JsonProperty("labelfor")
  LABELFOR,
  @JsonProperty("labelwrapped")
  LABELWRAPPED,
  @JsonProperty("legend")
  LEGEND,
  @JsonProperty("rubyannotation")
  RUBYANNOTATION,
  @JsonProperty("tablecaption")
  TABLECAPTION,
  @JsonProperty("title")
  TITLE,
  @JsonProperty("other")
  OTHER,
}
