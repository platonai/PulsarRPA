@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.network

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Details of a signed certificate timestamp (SCT).
 */
data class SignedCertificateTimestamp(
  @param:JsonProperty("status")
  val status: String,
  @param:JsonProperty("origin")
  val origin: String,
  @param:JsonProperty("logDescription")
  val logDescription: String,
  @param:JsonProperty("logId")
  val logId: String,
  @param:JsonProperty("timestamp")
  val timestamp: Double,
  @param:JsonProperty("hashAlgorithm")
  val hashAlgorithm: String,
  @param:JsonProperty("signatureAlgorithm")
  val signatureAlgorithm: String,
  @param:JsonProperty("signatureData")
  val signatureData: String,
)
