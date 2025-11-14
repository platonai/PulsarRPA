@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.security

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.security.InsecureContentStatus
import ai.platon.cdt.kt.protocol.types.security.SecurityState
import ai.platon.cdt.kt.protocol.types.security.SecurityStateExplanation
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.String
import kotlin.collections.List

/**
 * The security state of the page changed.
 */
data class SecurityStateChanged(
  @param:JsonProperty("securityState")
  val securityState: SecurityState,
  @param:JsonProperty("schemeIsCryptographic")
  @Deprecated("Deprecated by protocol")
  val schemeIsCryptographic: Boolean,
  @param:JsonProperty("explanations")
  val explanations: List<SecurityStateExplanation>,
  @param:JsonProperty("insecureContentStatus")
  @Deprecated("Deprecated by protocol")
  val insecureContentStatus: InsecureContentStatus,
  @param:JsonProperty("summary")
  @param:Optional
  val summary: String? = null,
)
