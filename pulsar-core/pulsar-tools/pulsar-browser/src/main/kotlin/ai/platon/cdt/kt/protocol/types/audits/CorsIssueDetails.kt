package ai.platon.cdt.kt.protocol.types.audits

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.network.ClientSecurityState
import ai.platon.cdt.kt.protocol.types.network.CorsErrorStatus
import ai.platon.cdt.kt.protocol.types.network.IPAddressSpace
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String

/**
 * Details for a CORS related issue, e.g. a warning or error related to
 * CORS RFC1918 enforcement.
 */
public data class CorsIssueDetails(
  @JsonProperty("corsErrorStatus")
  public val corsErrorStatus: CorsErrorStatus,
  @JsonProperty("isWarning")
  public val isWarning: Boolean,
  @JsonProperty("request")
  public val request: AffectedRequest,
  @JsonProperty("initiatorOrigin")
  @Optional
  public val initiatorOrigin: String? = null,
  @JsonProperty("resourceIPAddressSpace")
  @Optional
  public val resourceIPAddressSpace: IPAddressSpace? = null,
  @JsonProperty("clientSecurityState")
  @Optional
  public val clientSecurityState: ClientSecurityState? = null,
)
