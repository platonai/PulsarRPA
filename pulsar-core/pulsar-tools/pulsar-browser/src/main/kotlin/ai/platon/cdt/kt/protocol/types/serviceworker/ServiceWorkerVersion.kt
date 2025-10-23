@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.serviceworker

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String
import kotlin.collections.List

/**
 * ServiceWorker version.
 */
data class ServiceWorkerVersion(
  @param:JsonProperty("versionId")
  val versionId: String,
  @param:JsonProperty("registrationId")
  val registrationId: String,
  @param:JsonProperty("scriptURL")
  val scriptURL: String,
  @param:JsonProperty("runningStatus")
  val runningStatus: ServiceWorkerVersionRunningStatus,
  @param:JsonProperty("status")
  val status: ServiceWorkerVersionStatus,
  @param:JsonProperty("scriptLastModified")
  @param:Optional
  val scriptLastModified: Double? = null,
  @param:JsonProperty("scriptResponseTime")
  @param:Optional
  val scriptResponseTime: Double? = null,
  @param:JsonProperty("controlledClients")
  @param:Optional
  val controlledClients: List<String>? = null,
  @param:JsonProperty("targetId")
  @param:Optional
  val targetId: String? = null,
)
