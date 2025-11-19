@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.network

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Field type for a signed exchange related error.
 */
public enum class SignedExchangeErrorField {
  @JsonProperty("signatureSig")
  SIGNATURE_SIG,
  @JsonProperty("signatureIntegrity")
  SIGNATURE_INTEGRITY,
  @JsonProperty("signatureCertUrl")
  SIGNATURE_CERT_URL,
  @JsonProperty("signatureCertSha256")
  SIGNATURE_CERT_SHA_256,
  @JsonProperty("signatureValidityUrl")
  SIGNATURE_VALIDITY_URL,
  @JsonProperty("signatureTimestamps")
  SIGNATURE_TIMESTAMPS,
}
