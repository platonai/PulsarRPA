package ai.platon.cdt.kt.protocol.types.network

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.Int
import kotlin.String
import kotlin.collections.List

/**
 * Security details about a request.
 */
public data class SecurityDetails(
  @JsonProperty("protocol")
  public val protocol: String,
  @JsonProperty("keyExchange")
  public val keyExchange: String,
  @JsonProperty("keyExchangeGroup")
  @Optional
  public val keyExchangeGroup: String? = null,
  @JsonProperty("cipher")
  public val cipher: String,
  @JsonProperty("mac")
  @Optional
  public val mac: String? = null,
  @JsonProperty("certificateId")
  public val certificateId: Int,
  @JsonProperty("subjectName")
  public val subjectName: String,
  @JsonProperty("sanList")
  public val sanList: List<String>,
  @JsonProperty("issuer")
  public val issuer: String,
  @JsonProperty("validFrom")
  public val validFrom: Double,
  @JsonProperty("validTo")
  public val validTo: Double,
  @JsonProperty("signedCertificateTimestampList")
  public val signedCertificateTimestampList: List<SignedCertificateTimestamp>,
  @JsonProperty("certificateTransparencyCompliance")
  public val certificateTransparencyCompliance: CertificateTransparencyCompliance,
)
