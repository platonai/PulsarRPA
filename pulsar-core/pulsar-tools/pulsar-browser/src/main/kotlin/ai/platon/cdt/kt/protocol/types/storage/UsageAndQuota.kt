package ai.platon.cdt.kt.protocol.types.storage

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Double
import kotlin.collections.List

public data class UsageAndQuota(
  @JsonProperty("usage")
  public val usage: Double,
  @JsonProperty("quota")
  public val quota: Double,
  @JsonProperty("overrideActive")
  public val overrideActive: Boolean,
  @JsonProperty("usageBreakdown")
  public val usageBreakdown: List<UsageForType>,
)
