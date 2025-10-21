package ai.platon.cdt.kt.protocol.types.page

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Indicates whether the frame is a secure context and why it is the case.
 */
public enum class SecureContextType {
  @JsonProperty("Secure")
  SECURE,
  @JsonProperty("SecureLocalhost")
  SECURE_LOCALHOST,
  @JsonProperty("InsecureScheme")
  INSECURE_SCHEME,
  @JsonProperty("InsecureAncestor")
  INSECURE_ANCESTOR,
}
