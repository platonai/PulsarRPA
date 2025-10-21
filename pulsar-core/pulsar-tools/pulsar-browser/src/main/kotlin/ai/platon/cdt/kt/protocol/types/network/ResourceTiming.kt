package ai.platon.cdt.kt.protocol.types.network

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double

/**
 * Timing information for the request.
 */
public data class ResourceTiming(
  @JsonProperty("requestTime")
  public val requestTime: Double,
  @JsonProperty("proxyStart")
  public val proxyStart: Double,
  @JsonProperty("proxyEnd")
  public val proxyEnd: Double,
  @JsonProperty("dnsStart")
  public val dnsStart: Double,
  @JsonProperty("dnsEnd")
  public val dnsEnd: Double,
  @JsonProperty("connectStart")
  public val connectStart: Double,
  @JsonProperty("connectEnd")
  public val connectEnd: Double,
  @JsonProperty("sslStart")
  public val sslStart: Double,
  @JsonProperty("sslEnd")
  public val sslEnd: Double,
  @JsonProperty("workerStart")
  @Experimental
  public val workerStart: Double,
  @JsonProperty("workerReady")
  @Experimental
  public val workerReady: Double,
  @JsonProperty("workerFetchStart")
  @Experimental
  public val workerFetchStart: Double,
  @JsonProperty("workerRespondWithSettled")
  @Experimental
  public val workerRespondWithSettled: Double,
  @JsonProperty("sendStart")
  public val sendStart: Double,
  @JsonProperty("sendEnd")
  public val sendEnd: Double,
  @JsonProperty("pushStart")
  @Experimental
  public val pushStart: Double,
  @JsonProperty("pushEnd")
  @Experimental
  public val pushEnd: Double,
  @JsonProperty("receiveHeadersEnd")
  public val receiveHeadersEnd: Double,
)
