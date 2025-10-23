@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.domdebugger

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * CSP Violation type.
 */
public enum class CSPViolationType {
  @JsonProperty("trustedtype-sink-violation")
  TRUSTEDTYPE_SINK_VIOLATION,
  @JsonProperty("trustedtype-policy-violation")
  TRUSTEDTYPE_POLICY_VIOLATION,
}
