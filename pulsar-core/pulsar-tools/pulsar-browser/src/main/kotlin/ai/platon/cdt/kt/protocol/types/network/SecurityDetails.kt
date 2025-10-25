@file:Suppress("unused")
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
data class SecurityDetails(
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
  @param:JsonProperty("certificateId")
  val certificateId: Int,
  @param:JsonProperty("subjectName")
  val subjectName: String,
  @param:JsonProperty("sanList")
  val sanList: List<String>,
  @param:JsonProperty("issuer")
  val issuer: String,
  @param:JsonProperty("validFrom")
  val validFrom: Double,
  @param:JsonProperty("validTo")
  val validTo: Double,
  @param:JsonProperty("signedCertificateTimestampList")
  val signedCertificateTimestampList: List<SignedCertificateTimestamp>,
  @param:JsonProperty("certificateTransparencyCompliance")
  val certificateTransparencyCompliance: CertificateTransparencyCompliance,
)
