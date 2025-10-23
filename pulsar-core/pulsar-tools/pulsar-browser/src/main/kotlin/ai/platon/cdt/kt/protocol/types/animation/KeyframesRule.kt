@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.animation

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

/**
 * Keyframes Rule
 */
data class KeyframesRule(
  @param:JsonProperty("name")
  @param:Optional
  val name: String? = null,
  @param:JsonProperty("keyframes")
  val keyframes: List<KeyframeStyle>,
)
