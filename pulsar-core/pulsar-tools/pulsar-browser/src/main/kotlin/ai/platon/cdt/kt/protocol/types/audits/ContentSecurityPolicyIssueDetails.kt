package ai.platon.cdt.kt.protocol.types.audits

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Int
import kotlin.String

public data class ContentSecurityPolicyIssueDetails(
  @JsonProperty("blockedURL")
  @Optional
  public val blockedURL: String? = null,
  @JsonProperty("violatedDirective")
  public val violatedDirective: String,
  @JsonProperty("isReportOnly")
  public val isReportOnly: Boolean,
  @JsonProperty("contentSecurityPolicyViolationType")
  public val contentSecurityPolicyViolationType: ContentSecurityPolicyViolationType,
  @JsonProperty("frameAncestor")
  @Optional
  public val frameAncestor: AffectedFrame? = null,
  @JsonProperty("sourceCodeLocation")
  @Optional
  public val sourceCodeLocation: SourceCodeLocation? = null,
  @JsonProperty("violatingNodeId")
  @Optional
  public val violatingNodeId: Int? = null,
)
