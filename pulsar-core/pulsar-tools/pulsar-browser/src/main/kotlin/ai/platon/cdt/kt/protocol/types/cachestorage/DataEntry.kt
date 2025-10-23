@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.cachestorage

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.Int
import kotlin.String
import kotlin.collections.List

/**
 * Data entry.
 */
data class DataEntry(
  @param:JsonProperty("requestURL")
  val requestURL: String,
  @param:JsonProperty("requestMethod")
  val requestMethod: String,
  @param:JsonProperty("requestHeaders")
  val requestHeaders: List<Header>,
  @param:JsonProperty("responseTime")
  val responseTime: Double,
  @param:JsonProperty("responseStatus")
  val responseStatus: Int,
  @param:JsonProperty("responseStatusText")
  val responseStatusText: String,
  @param:JsonProperty("responseType")
  val responseType: CachedResponseType,
  @param:JsonProperty("responseHeaders")
  val responseHeaders: List<Header>,
)
