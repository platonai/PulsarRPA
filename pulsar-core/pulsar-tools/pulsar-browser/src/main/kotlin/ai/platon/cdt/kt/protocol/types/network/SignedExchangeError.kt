package ai.platon.cdt.kt.protocol.types.network

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Information about a signed exchange response.
 */
@Experimental
public data class SignedExchangeError(
  @JsonProperty("message")
  public val message: String,
  @JsonProperty("signatureIndex")
  @Optional
  public val signatureIndex: Int? = null,
  @JsonProperty("errorField")
  @Optional
  public val errorField: SignedExchangeErrorField? = null,
)
