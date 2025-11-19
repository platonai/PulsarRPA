@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.debugger

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Type of the debug symbols.
 */
public enum class DebugSymbolsType {
  @JsonProperty("None")
  NONE,
  @JsonProperty("SourceMap")
  SOURCE_MAP,
  @JsonProperty("EmbeddedDWARF")
  EMBEDDED_DWARF,
  @JsonProperty("ExternalDWARF")
  EXTERNAL_DWARF,
}
