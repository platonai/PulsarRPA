@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.serviceworker

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String

/**
 * ServiceWorker registration.
 */
data class ServiceWorkerRegistration(
  @param:JsonProperty("registrationId")
  val registrationId: String,
  @param:JsonProperty("scopeURL")
  val scopeURL: String,
  @param:JsonProperty("isDeleted")
  val isDeleted: Boolean,
)
