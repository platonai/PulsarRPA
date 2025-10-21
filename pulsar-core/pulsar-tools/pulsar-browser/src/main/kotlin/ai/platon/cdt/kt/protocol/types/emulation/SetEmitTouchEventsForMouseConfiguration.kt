package ai.platon.cdt.kt.protocol.types.emulation

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Touch/gesture events configuration. Default: current platform.
 */
public enum class SetEmitTouchEventsForMouseConfiguration {
  @JsonProperty("mobile")
  MOBILE,
  @JsonProperty("desktop")
  DESKTOP,
}
