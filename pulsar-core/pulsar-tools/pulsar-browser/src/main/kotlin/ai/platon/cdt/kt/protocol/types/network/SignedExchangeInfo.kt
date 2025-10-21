package ai.platon.cdt.kt.protocol.types.network

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.collections.List

/**
 * Information about a signed exchange response.
 */
@Experimental
public data class SignedExchangeInfo(
  @JsonProperty("outerResponse")
  public val outerResponse: Response,
  @JsonProperty("header")
  @Optional
  public val `header`: SignedExchangeHeader? = null,
  @JsonProperty("securityDetails")
  @Optional
  public val securityDetails: SecurityDetails? = null,
  @JsonProperty("errors")
  @Optional
  public val errors: List<SignedExchangeError>? = null,
)
