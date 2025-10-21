package ai.platon.cdt.kt.protocol.types.backgroundservice

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String
import kotlin.collections.List

public data class BackgroundServiceEvent(
  @JsonProperty("timestamp")
  public val timestamp: Double,
  @JsonProperty("origin")
  public val origin: String,
  @JsonProperty("serviceWorkerRegistrationId")
  public val serviceWorkerRegistrationId: String,
  @JsonProperty("service")
  public val service: ServiceName,
  @JsonProperty("eventName")
  public val eventName: String,
  @JsonProperty("instanceId")
  public val instanceId: String,
  @JsonProperty("eventMetadata")
  public val eventMetadata: List<EventMetadata>,
)
