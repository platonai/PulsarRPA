@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.audits

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Int
import kotlin.String

data class ContentSecurityPolicyIssueDetails(
  @param:JsonProperty("blockedURL")
  @param:Optional
  val blockedURL: String? = null,
  @param:JsonProperty("violatedDirective")
  val violatedDirective: String,
  @param:JsonProperty("isReportOnly")
  val isReportOnly: Boolean,
  @param:JsonProperty("contentSecurityPolicyViolationType")
  val contentSecurityPolicyViolationType: ContentSecurityPolicyViolationType,
  @param:JsonProperty("frameAncestor")
  @param:Optional
  val frameAncestor: AffectedFrame? = null,
  @param:JsonProperty("sourceCodeLocation")
  @param:Optional
  val sourceCodeLocation: SourceCodeLocation? = null,
  @param:JsonProperty("violatingNodeId")
  @param:Optional
  val violatingNodeId: Int? = null,
)
