package ai.platon.cdt.kt.protocol.types.network

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Any
import kotlin.Boolean
import kotlin.Double
import kotlin.String
import kotlin.collections.Map

/**
 * An object providing the result of a network resource load.
 */
@Experimental
public data class LoadNetworkResourcePageResult(
  @JsonProperty("success")
  public val success: Boolean,
  @JsonProperty("netError")
  @Optional
  public val netError: Double? = null,
  @JsonProperty("netErrorName")
  @Optional
  public val netErrorName: String? = null,
  @JsonProperty("httpStatusCode")
  @Optional
  public val httpStatusCode: Double? = null,
  @JsonProperty("stream")
  @Optional
  public val stream: String? = null,
  @JsonProperty("headers")
  @Optional
  public val headers: Map<String, Any>? = null,
)
