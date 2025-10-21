package ai.platon.cdt.kt.protocol.types.network

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.security.SecurityState
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Any
import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.String
import kotlin.collections.Map

/**
 * HTTP response data.
 */
public data class Response(
  @JsonProperty("url")
  public val url: String,
  @JsonProperty("status")
  public val status: Int,
  @JsonProperty("statusText")
  public val statusText: String,
  @JsonProperty("headers")
  public val headers: Map<String, Any?>,
  @JsonProperty("headersText")
  @Optional
  public val headersText: String? = null,
  @JsonProperty("mimeType")
  public val mimeType: String,
  @JsonProperty("requestHeaders")
  @Optional
  public val requestHeaders: Map<String, Any?>? = null,
  @JsonProperty("requestHeadersText")
  @Optional
  public val requestHeadersText: String? = null,
  @JsonProperty("connectionReused")
  public val connectionReused: Boolean,
  @JsonProperty("connectionId")
  public val connectionId: Double,
  @JsonProperty("remoteIPAddress")
  @Optional
  public val remoteIPAddress: String? = null,
  @JsonProperty("remotePort")
  @Optional
  public val remotePort: Int? = null,
  @JsonProperty("fromDiskCache")
  @Optional
  public val fromDiskCache: Boolean? = null,
  @JsonProperty("fromServiceWorker")
  @Optional
  public val fromServiceWorker: Boolean? = null,
  @JsonProperty("fromPrefetchCache")
  @Optional
  public val fromPrefetchCache: Boolean? = null,
  @JsonProperty("encodedDataLength")
  public val encodedDataLength: Double,
  @JsonProperty("timing")
  @Optional
  public val timing: ResourceTiming? = null,
  @JsonProperty("serviceWorkerResponseSource")
  @Optional
  public val serviceWorkerResponseSource: ServiceWorkerResponseSource? = null,
  @JsonProperty("responseTime")
  @Optional
  public val responseTime: Double? = null,
  @JsonProperty("cacheStorageCacheName")
  @Optional
  public val cacheStorageCacheName: String? = null,
  @JsonProperty("protocol")
  @Optional
  public val protocol: String? = null,
  @JsonProperty("securityState")
  public val securityState: SecurityState,
  @JsonProperty("securityDetails")
  @Optional
  public val securityDetails: SecurityDetails? = null,
)
