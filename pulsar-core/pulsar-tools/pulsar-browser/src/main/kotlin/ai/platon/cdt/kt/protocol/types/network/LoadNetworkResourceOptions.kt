package ai.platon.cdt.kt.protocol.types.network

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean

/**
 * An options object that may be extended later to better support CORS,
 * CORB and streaming.
 */
@Experimental
public data class LoadNetworkResourceOptions(
  @JsonProperty("disableCache")
  public val disableCache: Boolean,
  @JsonProperty("includeCredentials")
  public val includeCredentials: Boolean,
)
