@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.network

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String
import kotlin.collections.List

/**
 * Information about a signed exchange signature.
 * https://wicg.github.io/webpackage/draft-yasskin-httpbis-origin-signed-exchanges-impl.html#rfc.section.3.1
 */
@Experimental
data class SignedExchangeSignature(
  @param:JsonProperty("label")
  val label: String,
  @param:JsonProperty("signature")
  val signature: String,
  @param:JsonProperty("integrity")
  val integrity: String,
  @param:JsonProperty("certUrl")
  @param:Optional
  val certUrl: String? = null,
  @param:JsonProperty("certSha256")
  @param:Optional
  val certSha256: String? = null,
  @param:JsonProperty("validityUrl")
  val validityUrl: String,
  @param:JsonProperty("date")
  val date: Int,
  @param:JsonProperty("expires")
  val expires: Int,
  @param:JsonProperty("certificates")
  @param:Optional
  val certificates: List<String>? = null,
)
