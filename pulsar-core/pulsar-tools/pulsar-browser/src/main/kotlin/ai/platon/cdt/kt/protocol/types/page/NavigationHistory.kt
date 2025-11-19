@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.page

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.collections.List

data class NavigationHistory(
  @param:JsonProperty("currentIndex")
  val currentIndex: Int,
  @param:JsonProperty("entries")
  val entries: List<NavigationEntry>,
)
