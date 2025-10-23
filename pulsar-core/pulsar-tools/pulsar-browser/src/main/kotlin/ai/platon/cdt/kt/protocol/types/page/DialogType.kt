@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.page

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Javascript dialog type.
 */
public enum class DialogType {
  @JsonProperty("alert")
  ALERT,
  @JsonProperty("confirm")
  CONFIRM,
  @JsonProperty("prompt")
  PROMPT,
  @JsonProperty("beforeunload")
  BEFOREUNLOAD,
}
