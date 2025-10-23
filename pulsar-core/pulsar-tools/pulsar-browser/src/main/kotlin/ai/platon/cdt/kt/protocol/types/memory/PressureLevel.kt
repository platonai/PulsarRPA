@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.memory

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Memory pressure level.
 */
public enum class PressureLevel {
  @JsonProperty("moderate")
  MODERATE,
  @JsonProperty("critical")
  CRITICAL,
}
