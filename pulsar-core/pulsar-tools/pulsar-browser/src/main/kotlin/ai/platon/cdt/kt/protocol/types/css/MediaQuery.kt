@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.css

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.collections.List

/**
 * Media query descriptor.
 */
data class MediaQuery(
  @param:JsonProperty("expressions")
  val expressions: List<MediaQueryExpression>,
  @param:JsonProperty("active")
  val active: Boolean,
)
