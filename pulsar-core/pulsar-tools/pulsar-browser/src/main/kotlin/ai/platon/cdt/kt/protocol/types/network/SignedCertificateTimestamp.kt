package ai.platon.cdt.kt.protocol.types.network

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Details of a signed certificate timestamp (SCT).
 */
public data class SignedCertificateTimestamp(
  @JsonProperty("status")
  public val status: String,
  @JsonProperty("origin")
  public val origin: String,
  @JsonProperty("logDescription")
  public val logDescription: String,
  @JsonProperty("logId")
  public val logId: String,
  @JsonProperty("timestamp")
  public val timestamp: Double,
  @JsonProperty("hashAlgorithm")
  public val hashAlgorithm: String,
  @JsonProperty("signatureAlgorithm")
  public val signatureAlgorithm: String,
  @JsonProperty("signatureData")
  public val signatureData: String,
)
