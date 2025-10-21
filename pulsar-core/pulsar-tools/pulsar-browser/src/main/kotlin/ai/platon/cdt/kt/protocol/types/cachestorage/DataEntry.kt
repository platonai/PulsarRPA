package ai.platon.cdt.kt.protocol.types.cachestorage

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.Int
import kotlin.String
import kotlin.collections.List

/**
 * Data entry.
 */
public data class DataEntry(
  @JsonProperty("requestURL")
  public val requestURL: String,
  @JsonProperty("requestMethod")
  public val requestMethod: String,
  @JsonProperty("requestHeaders")
  public val requestHeaders: List<Header>,
  @JsonProperty("responseTime")
  public val responseTime: Double,
  @JsonProperty("responseStatus")
  public val responseStatus: Int,
  @JsonProperty("responseStatusText")
  public val responseStatusText: String,
  @JsonProperty("responseType")
  public val responseType: CachedResponseType,
  @JsonProperty("responseHeaders")
  public val responseHeaders: List<Header>,
)
