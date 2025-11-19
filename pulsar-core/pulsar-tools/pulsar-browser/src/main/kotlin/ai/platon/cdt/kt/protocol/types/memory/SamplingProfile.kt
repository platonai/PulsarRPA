@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.memory

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.collections.List

/**
 * Array of heap profile samples.
 */
data class SamplingProfile(
  @param:JsonProperty("samples")
  val samples: List<SamplingProfileNode>,
  @param:JsonProperty("modules")
  val modules: List<Module>,
)
