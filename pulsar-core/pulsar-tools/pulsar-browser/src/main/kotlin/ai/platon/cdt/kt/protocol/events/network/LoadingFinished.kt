package ai.platon.cdt.kt.protocol.events.network

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Double
import kotlin.String

/**
 * Fired when HTTP request has finished loading.
 */
public data class LoadingFinished(
  @JsonProperty("requestId")
  public val requestId: String,
  @JsonProperty("timestamp")
  public val timestamp: Double,
  @JsonProperty("encodedDataLength")
  public val encodedDataLength: Double,
  @JsonProperty("shouldReportCorbBlocking")
  @Optional
  public val shouldReportCorbBlocking: Boolean? = null,
)
