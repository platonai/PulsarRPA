@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.security

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

/**
 * Security state information about the page.
 */
@Experimental
data class VisibleSecurityState(
  @param:JsonProperty("securityState")
  val securityState: SecurityState,
  @param:JsonProperty("certificateSecurityState")
  @param:Optional
  val certificateSecurityState: CertificateSecurityState? = null,
  @param:JsonProperty("safetyTipInfo")
  @param:Optional
  val safetyTipInfo: SafetyTipInfo? = null,
  @param:JsonProperty("securityStateIssueIds")
  val securityStateIssueIds: List<String>,
)
