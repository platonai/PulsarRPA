@file:Suppress("unused")
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
data class Response(
  @param:JsonProperty("url")
  val url: String,
  @param:JsonProperty("status")
  val status: Int,
  @param:JsonProperty("statusText")
  val statusText: String,
  @param:JsonProperty("headers")
  val headers: Map<String, Any?>,
  @param:JsonProperty("headersText")
  @param:Optional
  val headersText: String? = null,
  @param:JsonProperty("mimeType")
  val mimeType: String,
  @param:JsonProperty("requestHeaders")
  @param:Optional
  val requestHeaders: Map<String, Any?>? = null,
  @param:JsonProperty("requestHeadersText")
  @param:Optional
  val requestHeadersText: String? = null,
  @param:JsonProperty("connectionReused")
  val connectionReused: Boolean,
  @param:JsonProperty("connectionId")
  val connectionId: Double,
  @param:JsonProperty("remoteIPAddress")
  @param:Optional
  val remoteIPAddress: String? = null,
  @param:JsonProperty("remotePort")
  @param:Optional
  val remotePort: Int? = null,
  @param:JsonProperty("fromDiskCache")
  @param:Optional
  val fromDiskCache: Boolean? = null,
  @param:JsonProperty("fromServiceWorker")
  @param:Optional
  val fromServiceWorker: Boolean? = null,
  @param:JsonProperty("fromPrefetchCache")
  @param:Optional
  val fromPrefetchCache: Boolean? = null,
  @param:JsonProperty("encodedDataLength")
  val encodedDataLength: Double,
  @param:JsonProperty("timing")
  @param:Optional
  val timing: ResourceTiming? = null,
  @param:JsonProperty("serviceWorkerResponseSource")
  @param:Optional
  val serviceWorkerResponseSource: ServiceWorkerResponseSource? = null,
  @param:JsonProperty("responseTime")
  @param:Optional
  val responseTime: Double? = null,
  @param:JsonProperty("cacheStorageCacheName")
  @param:Optional
  val cacheStorageCacheName: String? = null,
  @param:JsonProperty("protocol")
  @param:Optional
  val protocol: String? = null,
  @param:JsonProperty("securityState")
  val securityState: SecurityState,
  @param:JsonProperty("securityDetails")
  @param:Optional
  val securityDetails: SecurityDetails? = null,
)
