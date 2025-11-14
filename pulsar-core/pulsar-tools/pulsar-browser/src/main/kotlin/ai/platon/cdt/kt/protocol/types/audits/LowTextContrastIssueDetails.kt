@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.audits

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.Int
import kotlin.String

data class LowTextContrastIssueDetails(
  @param:JsonProperty("violatingNodeId")
  val violatingNodeId: Int,
  @param:JsonProperty("violatingNodeSelector")
  val violatingNodeSelector: String,
  @param:JsonProperty("contrastRatio")
  val contrastRatio: Double,
  @param:JsonProperty("thresholdAA")
  val thresholdAA: Double,
  @param:JsonProperty("thresholdAAA")
  val thresholdAAA: Double,
  @param:JsonProperty("fontSize")
  val fontSize: String,
  @param:JsonProperty("fontWeight")
  val fontWeight: String,
)
