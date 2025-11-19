@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.page

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Navigation history entry.
 */
data class NavigationEntry(
  @param:JsonProperty("id")
  val id: Int,
  @param:JsonProperty("url")
  val url: String,
  @param:JsonProperty("userTypedURL")
  val userTypedURL: String,
  @param:JsonProperty("title")
  val title: String,
  @param:JsonProperty("transitionType")
  val transitionType: TransitionType,
)
