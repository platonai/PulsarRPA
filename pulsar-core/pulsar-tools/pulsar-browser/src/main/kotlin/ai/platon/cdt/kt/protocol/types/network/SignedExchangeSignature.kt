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
public data class SignedExchangeSignature(
  @JsonProperty("label")
  public val label: String,
  @JsonProperty("signature")
  public val signature: String,
  @JsonProperty("integrity")
  public val integrity: String,
  @JsonProperty("certUrl")
  @Optional
  public val certUrl: String? = null,
  @JsonProperty("certSha256")
  @Optional
  public val certSha256: String? = null,
  @JsonProperty("validityUrl")
  public val validityUrl: String,
  @JsonProperty("date")
  public val date: Int,
  @JsonProperty("expires")
  public val expires: Int,
  @JsonProperty("certificates")
  @Optional
  public val certificates: List<String>? = null,
)
