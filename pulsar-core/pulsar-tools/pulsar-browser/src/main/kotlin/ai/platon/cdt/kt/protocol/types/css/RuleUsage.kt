@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.css

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Double
import kotlin.String

/**
 * CSS coverage information.
 */
data class RuleUsage(
  @param:JsonProperty("styleSheetId")
  val styleSheetId: String,
  @param:JsonProperty("startOffset")
  val startOffset: Double,
  @param:JsonProperty("endOffset")
  val endOffset: Double,
  @param:JsonProperty("used")
  val used: Boolean,
)
