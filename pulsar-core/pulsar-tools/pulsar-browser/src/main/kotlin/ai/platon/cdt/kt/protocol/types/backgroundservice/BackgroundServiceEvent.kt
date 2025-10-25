@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.backgroundservice

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String
import kotlin.collections.List

data class BackgroundServiceEvent(
  @param:JsonProperty("timestamp")
  val timestamp: Double,
  @param:JsonProperty("origin")
  val origin: String,
  @param:JsonProperty("serviceWorkerRegistrationId")
  val serviceWorkerRegistrationId: String,
  @param:JsonProperty("service")
  val service: ServiceName,
  @param:JsonProperty("eventName")
  val eventName: String,
  @param:JsonProperty("instanceId")
  val instanceId: String,
  @param:JsonProperty("eventMetadata")
  val eventMetadata: List<EventMetadata>,
)
