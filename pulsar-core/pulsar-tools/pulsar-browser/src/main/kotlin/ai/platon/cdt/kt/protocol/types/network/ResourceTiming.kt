@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.network

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double

/**
 * Timing information for the request.
 */
data class ResourceTiming(
  @param:JsonProperty("requestTime")
  val requestTime: Double,
  @param:JsonProperty("proxyStart")
  val proxyStart: Double,
  @param:JsonProperty("proxyEnd")
  val proxyEnd: Double,
  @param:JsonProperty("dnsStart")
  val dnsStart: Double,
  @param:JsonProperty("dnsEnd")
  val dnsEnd: Double,
  @param:JsonProperty("connectStart")
  val connectStart: Double,
  @param:JsonProperty("connectEnd")
  val connectEnd: Double,
  @param:JsonProperty("sslStart")
  val sslStart: Double,
  @param:JsonProperty("sslEnd")
  val sslEnd: Double,
  @param:JsonProperty("workerStart")
  @param:Experimental
  val workerStart: Double,
  @param:JsonProperty("workerReady")
  @param:Experimental
  val workerReady: Double,
  @param:JsonProperty("workerFetchStart")
  @param:Experimental
  val workerFetchStart: Double,
  @param:JsonProperty("workerRespondWithSettled")
  @param:Experimental
  val workerRespondWithSettled: Double,
  @param:JsonProperty("sendStart")
  val sendStart: Double,
  @param:JsonProperty("sendEnd")
  val sendEnd: Double,
  @param:JsonProperty("pushStart")
  @param:Experimental
  val pushStart: Double,
  @param:JsonProperty("pushEnd")
  @param:Experimental
  val pushEnd: Double,
  @param:JsonProperty("receiveHeadersEnd")
  val receiveHeadersEnd: Double,
)
