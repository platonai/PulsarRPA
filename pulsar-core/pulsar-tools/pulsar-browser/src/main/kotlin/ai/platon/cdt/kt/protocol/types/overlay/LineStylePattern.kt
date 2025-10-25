@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.overlay

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * The line pattern (default: solid)
 */
public enum class LineStylePattern {
  @JsonProperty("dashed")
  DASHED,
  @JsonProperty("dotted")
  DOTTED,
}
