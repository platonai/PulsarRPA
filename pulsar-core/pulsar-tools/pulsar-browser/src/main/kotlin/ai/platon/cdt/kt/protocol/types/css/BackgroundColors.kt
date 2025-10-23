@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.css

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

data class BackgroundColors(
  @param:JsonProperty("backgroundColors")
  @param:Optional
  val backgroundColors: List<String>? = null,
  @param:JsonProperty("computedFontSize")
  @param:Optional
  val computedFontSize: String? = null,
  @param:JsonProperty("computedFontWeight")
  @param:Optional
  val computedFontWeight: String? = null,
)
