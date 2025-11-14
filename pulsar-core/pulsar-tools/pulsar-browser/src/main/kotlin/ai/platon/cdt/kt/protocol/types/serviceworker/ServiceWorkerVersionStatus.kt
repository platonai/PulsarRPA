@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.serviceworker

import com.fasterxml.jackson.`annotation`.JsonProperty

public enum class ServiceWorkerVersionStatus {
  @JsonProperty("new")
  NEW,
  @JsonProperty("installing")
  INSTALLING,
  @JsonProperty("installed")
  INSTALLED,
  @JsonProperty("activating")
  ACTIVATING,
  @JsonProperty("activated")
  ACTIVATED,
  @JsonProperty("redundant")
  REDUNDANT,
}
