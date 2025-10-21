package ai.platon.cdt.kt.protocol.types.log

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Violation type.
 */
public enum class ViolationSettingName {
  @JsonProperty("longTask")
  LONG_TASK,
  @JsonProperty("longLayout")
  LONG_LAYOUT,
  @JsonProperty("blockedEvent")
  BLOCKED_EVENT,
  @JsonProperty("blockedParser")
  BLOCKED_PARSER,
  @JsonProperty("discouragedAPIUse")
  DISCOURAGED_API_USE,
  @JsonProperty("handler")
  HANDLER,
  @JsonProperty("recurringHandler")
  RECURRING_HANDLER,
}
