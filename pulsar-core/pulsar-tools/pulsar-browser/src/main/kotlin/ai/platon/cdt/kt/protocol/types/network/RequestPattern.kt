package ai.platon.cdt.kt.protocol.types.network

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Request pattern for interception.
 */
@Experimental
public data class RequestPattern(
  @JsonProperty("urlPattern")
  @Optional
  public val urlPattern: String? = null,
  @JsonProperty("resourceType")
  @Optional
  public val resourceType: ResourceType? = null,
  @JsonProperty("interceptionStage")
  @Optional
  public val interceptionStage: InterceptionStage? = null,
)
