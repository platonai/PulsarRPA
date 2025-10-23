@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.browser

import com.fasterxml.jackson.`annotation`.JsonProperty

public enum class PermissionSetting {
  @JsonProperty("granted")
  GRANTED,
  @JsonProperty("denied")
  DENIED,
  @JsonProperty("prompt")
  PROMPT,
}
