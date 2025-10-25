@file:Suppress("unused")
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
data class CorsIssueDetails(
  @param:JsonProperty("corsErrorStatus")
  val corsErrorStatus: CorsErrorStatus,
  @param:JsonProperty("isWarning")
  val isWarning: Boolean,
  @param:JsonProperty("request")
  val request: AffectedRequest,
  @param:JsonProperty("initiatorOrigin")
  @param:Optional
  val initiatorOrigin: String? = null,
  @param:JsonProperty("resourceIPAddressSpace")
  @param:Optional
  val resourceIPAddressSpace: IPAddressSpace? = null,
  @param:JsonProperty("clientSecurityState")
  @param:Optional
  val clientSecurityState: ClientSecurityState? = null,
)
