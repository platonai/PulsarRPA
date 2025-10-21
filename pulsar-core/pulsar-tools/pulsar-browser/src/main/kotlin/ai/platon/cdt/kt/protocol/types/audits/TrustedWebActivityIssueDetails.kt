package ai.platon.cdt.kt.protocol.types.audits

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

public data class TrustedWebActivityIssueDetails(
  @JsonProperty("url")
  public val url: String,
  @JsonProperty("violationType")
  public val violationType: TwaQualityEnforcementViolationType,
  @JsonProperty("httpStatusCode")
  @Optional
  public val httpStatusCode: Int? = null,
  @JsonProperty("packageName")
  @Optional
  public val packageName: String? = null,
  @JsonProperty("signature")
  @Optional
  public val signature: String? = null,
)
