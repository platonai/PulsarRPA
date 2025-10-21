package ai.platon.cdt.kt.protocol.types.overlay

import com.fasterxml.jackson.`annotation`.JsonProperty

public enum class ColorFormat {
  @JsonProperty("rgb")
  RGB,
  @JsonProperty("hsl")
  HSL,
  @JsonProperty("hex")
  HEX,
}
