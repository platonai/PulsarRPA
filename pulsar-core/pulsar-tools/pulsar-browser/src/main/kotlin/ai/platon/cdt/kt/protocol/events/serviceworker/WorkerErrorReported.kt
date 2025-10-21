package ai.platon.cdt.kt.protocol.events.serviceworker

import ai.platon.cdt.kt.protocol.types.serviceworker.ServiceWorkerErrorMessage
import com.fasterxml.jackson.`annotation`.JsonProperty

public data class WorkerErrorReported(
  @JsonProperty("errorMessage")
  public val errorMessage: ServiceWorkerErrorMessage,
)
