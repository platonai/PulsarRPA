@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.debugger

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Enum of possible script languages.
 */
public enum class ScriptLanguage {
  @JsonProperty("JavaScript")
  JAVA_SCRIPT,
  @JsonProperty("WebAssembly")
  WEB_ASSEMBLY,
}
