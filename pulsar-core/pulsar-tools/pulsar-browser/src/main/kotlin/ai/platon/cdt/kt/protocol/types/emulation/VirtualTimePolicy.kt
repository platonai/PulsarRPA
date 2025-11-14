@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.emulation

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * advance: If the scheduler runs out of immediate work, the virtual time base may fast forward to
 * allow the next delayed task (if any) to run; pause: The virtual time base may not advance;
 * pauseIfNetworkFetchesPending: The virtual time base may not advance if there are any pending
 * resource fetches.
 */
public enum class VirtualTimePolicy {
  @JsonProperty("advance")
  ADVANCE,
  @JsonProperty("pause")
  PAUSE,
  @JsonProperty("pauseIfNetworkFetchesPending")
  PAUSE_IF_NETWORK_FETCHES_PENDING,
}
