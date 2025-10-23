@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.log

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double

/**
 * Violation configuration setting.
 */
data class ViolationSetting(
  @param:JsonProperty("name")
  val name: ViolationSettingName,
  @param:JsonProperty("threshold")
  val threshold: Double,
)
