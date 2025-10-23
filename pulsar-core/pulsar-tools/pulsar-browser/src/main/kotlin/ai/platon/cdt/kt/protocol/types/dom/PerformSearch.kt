@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.dom

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

data class PerformSearch(
  @param:JsonProperty("searchId")
  val searchId: String,
  @param:JsonProperty("resultCount")
  val resultCount: Int,
)
