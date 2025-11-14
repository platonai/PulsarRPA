@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.debugger

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Scope type.
 */
public enum class ScopeType {
  @JsonProperty("global")
  GLOBAL,
  @JsonProperty("local")
  LOCAL,
  @JsonProperty("with")
  WITH,
  @JsonProperty("closure")
  CLOSURE,
  @JsonProperty("catch")
  CATCH,
  @JsonProperty("block")
  BLOCK,
  @JsonProperty("script")
  SCRIPT,
  @JsonProperty("eval")
  EVAL,
  @JsonProperty("module")
  MODULE,
  @JsonProperty("wasm-expression-stack")
  WASM_EXPRESSION_STACK,
}
