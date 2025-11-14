@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.serviceworker

import com.fasterxml.jackson.`annotation`.JsonProperty

public enum class ServiceWorkerVersionRunningStatus {
  @JsonProperty("stopped")
  STOPPED,
  @JsonProperty("starting")
  STARTING,
  @JsonProperty("running")
  RUNNING,
  @JsonProperty("stopping")
  STOPPING,
}
