@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.audits

import com.fasterxml.jackson.`annotation`.JsonProperty

public enum class ContentSecurityPolicyViolationType {
  @JsonProperty("kInlineViolation")
  K_INLINE_VIOLATION,
  @JsonProperty("kEvalViolation")
  K_EVAL_VIOLATION,
  @JsonProperty("kURLViolation")
  K_URL_VIOLATION,
  @JsonProperty("kTrustedTypesSinkViolation")
  K_TRUSTED_TYPES_SINK_VIOLATION,
  @JsonProperty("kTrustedTypesPolicyViolation")
  K_TRUSTED_TYPES_POLICY_VIOLATION,
}
