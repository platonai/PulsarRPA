@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.emulation

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

data class DisplayFeature(
  @param:JsonProperty("orientation")
  val orientation: DisplayFeatureOrientation,
  @param:JsonProperty("offset")
  val offset: Int,
  @param:JsonProperty("maskLength")
  val maskLength: Int,
)
