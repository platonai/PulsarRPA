@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.emulation

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Vision deficiency to emulate.
 */
public enum class SetEmulatedVisionDeficiencyType {
  @JsonProperty("none")
  NONE,
  @JsonProperty("achromatopsia")
  ACHROMATOPSIA,
  @JsonProperty("blurredVision")
  BLURRED_VISION,
  @JsonProperty("deuteranopia")
  DEUTERANOPIA,
  @JsonProperty("protanopia")
  PROTANOPIA,
  @JsonProperty("tritanopia")
  TRITANOPIA,
}
