package ai.platon.cdt.kt.protocol.types.serviceworker

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String
import kotlin.collections.List

/**
 * ServiceWorker version.
 */
public data class ServiceWorkerVersion(
  @JsonProperty("versionId")
  public val versionId: String,
  @JsonProperty("registrationId")
  public val registrationId: String,
  @JsonProperty("scriptURL")
  public val scriptURL: String,
  @JsonProperty("runningStatus")
  public val runningStatus: ServiceWorkerVersionRunningStatus,
  @JsonProperty("status")
  public val status: ServiceWorkerVersionStatus,
  @JsonProperty("scriptLastModified")
  @Optional
  public val scriptLastModified: Double? = null,
  @JsonProperty("scriptResponseTime")
  @Optional
  public val scriptResponseTime: Double? = null,
  @JsonProperty("controlledClients")
  @Optional
  public val controlledClients: List<String>? = null,
  @JsonProperty("targetId")
  @Optional
  public val targetId: String? = null,
)
