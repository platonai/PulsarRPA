package ai.platon.cdt.kt.protocol.types.security

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Double
import kotlin.String
import kotlin.collections.List

/**
 * Details about the security state of the page certificate.
 */
@Experimental
public data class CertificateSecurityState(
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
  @JsonProperty("certificate")
  public val certificate: List<String>,
  @JsonProperty("subjectName")
  public val subjectName: String,
  @JsonProperty("issuer")
  public val issuer: String,
  @JsonProperty("validFrom")
  public val validFrom: Double,
  @JsonProperty("validTo")
  public val validTo: Double,
  @JsonProperty("certificateNetworkError")
  @Optional
  public val certificateNetworkError: String? = null,
  @JsonProperty("certificateHasWeakSignature")
  public val certificateHasWeakSignature: Boolean,
  @JsonProperty("certificateHasSha1Signature")
  public val certificateHasSha1Signature: Boolean,
  @JsonProperty("modernSSL")
  public val modernSSL: Boolean,
  @JsonProperty("obsoleteSslProtocol")
  public val obsoleteSslProtocol: Boolean,
  @JsonProperty("obsoleteSslKeyExchange")
  public val obsoleteSslKeyExchange: Boolean,
  @JsonProperty("obsoleteSslCipher")
  public val obsoleteSslCipher: Boolean,
  @JsonProperty("obsoleteSslSignature")
  public val obsoleteSslSignature: Boolean,
)
