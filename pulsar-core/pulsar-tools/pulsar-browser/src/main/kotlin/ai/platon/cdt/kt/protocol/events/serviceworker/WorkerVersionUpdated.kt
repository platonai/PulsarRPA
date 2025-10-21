package ai.platon.cdt.kt.protocol.events.serviceworker

import ai.platon.cdt.kt.protocol.types.serviceworker.ServiceWorkerVersion
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.collections.List

public data class WorkerVersionUpdated(
  @JsonProperty("versions")
  public val versions: List<ServiceWorkerVersion>,
)
