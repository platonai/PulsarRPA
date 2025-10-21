package ai.platon.cdt.kt.protocol.types.security

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * The action to take when a certificate error occurs. continue will continue processing the
 * request and cancel will cancel the request.
 */
public enum class CertificateErrorAction {
  @JsonProperty("continue")
  CONTINUE,
  @JsonProperty("cancel")
  CANCEL,
}
