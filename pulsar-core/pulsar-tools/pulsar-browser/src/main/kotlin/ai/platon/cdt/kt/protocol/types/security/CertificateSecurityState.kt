@file:Suppress("unused")
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
data class CertificateSecurityState(
  @param:JsonProperty("protocol")
  val protocol: String,
  @param:JsonProperty("keyExchange")
  val keyExchange: String,
  @param:JsonProperty("keyExchangeGroup")
  @param:Optional
  val keyExchangeGroup: String? = null,
  @param:JsonProperty("cipher")
  val cipher: String,
  @param:JsonProperty("mac")
  @param:Optional
  val mac: String? = null,
  @param:JsonProperty("certificate")
  val certificate: List<String>,
  @param:JsonProperty("subjectName")
  val subjectName: String,
  @param:JsonProperty("issuer")
  val issuer: String,
  @param:JsonProperty("validFrom")
  val validFrom: Double,
  @param:JsonProperty("validTo")
  val validTo: Double,
  @param:JsonProperty("certificateNetworkError")
  @param:Optional
  val certificateNetworkError: String? = null,
  @param:JsonProperty("certificateHasWeakSignature")
  val certificateHasWeakSignature: Boolean,
  @param:JsonProperty("certificateHasSha1Signature")
  val certificateHasSha1Signature: Boolean,
  @param:JsonProperty("modernSSL")
  val modernSSL: Boolean,
  @param:JsonProperty("obsoleteSslProtocol")
  val obsoleteSslProtocol: Boolean,
  @param:JsonProperty("obsoleteSslKeyExchange")
  val obsoleteSslKeyExchange: Boolean,
  @param:JsonProperty("obsoleteSslCipher")
  val obsoleteSslCipher: Boolean,
  @param:JsonProperty("obsoleteSslSignature")
  val obsoleteSslSignature: Boolean,
)
