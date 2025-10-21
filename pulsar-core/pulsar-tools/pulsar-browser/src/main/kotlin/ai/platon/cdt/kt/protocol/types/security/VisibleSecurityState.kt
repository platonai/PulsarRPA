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
public data class VisibleSecurityState(
  @JsonProperty("securityState")
  public val securityState: SecurityState,
  @JsonProperty("certificateSecurityState")
  @Optional
  public val certificateSecurityState: CertificateSecurityState? = null,
  @JsonProperty("safetyTipInfo")
  @Optional
  public val safetyTipInfo: SafetyTipInfo? = null,
  @JsonProperty("securityStateIssueIds")
  public val securityStateIssueIds: List<String>,
)
