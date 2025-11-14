@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.headlessexperimental

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String

data class BeginFrame(
  @param:JsonProperty("hasDamage")
  val hasDamage: Boolean,
  @param:JsonProperty("screenshotData")
  @param:Optional
  val screenshotData: String? = null,
)
