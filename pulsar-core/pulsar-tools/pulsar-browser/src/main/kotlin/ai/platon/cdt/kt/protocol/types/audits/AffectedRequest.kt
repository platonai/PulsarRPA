package ai.platon.cdt.kt.protocol.types.audits

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Information about a request that is affected by an inspector issue.
 */
public data class AffectedRequest(
  @JsonProperty("requestId")
  public val requestId: String,
  @JsonProperty("url")
  @Optional
  public val url: String? = null,
)
