@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.storage

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Double
import kotlin.collections.List

data class UsageAndQuota(
  @param:JsonProperty("usage")
  val usage: Double,
  @param:JsonProperty("quota")
  val quota: Double,
  @param:JsonProperty("overrideActive")
  val overrideActive: Boolean,
  @param:JsonProperty("usageBreakdown")
  val usageBreakdown: List<UsageForType>,
)
