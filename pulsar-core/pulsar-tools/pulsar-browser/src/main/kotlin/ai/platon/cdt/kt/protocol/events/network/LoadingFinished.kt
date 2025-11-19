@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.network

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Double
import kotlin.String

/**
 * Fired when HTTP request has finished loading.
 */
data class LoadingFinished(
  @param:JsonProperty("requestId")
  val requestId: String,
  @param:JsonProperty("timestamp")
  val timestamp: Double,
  @param:JsonProperty("encodedDataLength")
  val encodedDataLength: Double,
  @param:JsonProperty("shouldReportCorbBlocking")
  @param:Optional
  val shouldReportCorbBlocking: Boolean? = null,
)
