@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.audits

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

data class TrustedWebActivityIssueDetails(
  @param:JsonProperty("url")
  val url: String,
  @param:JsonProperty("violationType")
  val violationType: TwaQualityEnforcementViolationType,
  @param:JsonProperty("httpStatusCode")
  @param:Optional
  val httpStatusCode: Int? = null,
  @param:JsonProperty("packageName")
  @param:Optional
  val packageName: String? = null,
  @param:JsonProperty("signature")
  @param:Optional
  val signature: String? = null,
)
