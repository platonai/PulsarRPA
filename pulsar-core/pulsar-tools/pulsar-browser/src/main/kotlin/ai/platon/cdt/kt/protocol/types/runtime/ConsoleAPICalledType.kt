package ai.platon.cdt.kt.protocol.types.runtime

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Type of the call.
 */
public enum class ConsoleAPICalledType {
  @JsonProperty("log")
  LOG,
  @JsonProperty("debug")
  DEBUG,
  @JsonProperty("info")
  INFO,
  @JsonProperty("error")
  ERROR,
  @JsonProperty("warning")
  WARNING,
  @JsonProperty("dir")
  DIR,
  @JsonProperty("dirxml")
  DIRXML,
  @JsonProperty("table")
  TABLE,
  @JsonProperty("trace")
  TRACE,
  @JsonProperty("clear")
  CLEAR,
  @JsonProperty("startGroup")
  START_GROUP,
  @JsonProperty("startGroupCollapsed")
  START_GROUP_COLLAPSED,
  @JsonProperty("endGroup")
  END_GROUP,
  @JsonProperty("assert")
  ASSERT,
  @JsonProperty("profile")
  PROFILE,
  @JsonProperty("profileEnd")
  PROFILE_END,
  @JsonProperty("count")
  COUNT,
  @JsonProperty("timeEnd")
  TIME_END,
}
