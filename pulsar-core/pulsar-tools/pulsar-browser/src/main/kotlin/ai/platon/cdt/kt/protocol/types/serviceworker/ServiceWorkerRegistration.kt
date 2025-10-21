package ai.platon.cdt.kt.protocol.types.serviceworker

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String

/**
 * ServiceWorker registration.
 */
public data class ServiceWorkerRegistration(
  @JsonProperty("registrationId")
  public val registrationId: String,
  @JsonProperty("scopeURL")
  public val scopeURL: String,
  @JsonProperty("isDeleted")
  public val isDeleted: Boolean,
)
