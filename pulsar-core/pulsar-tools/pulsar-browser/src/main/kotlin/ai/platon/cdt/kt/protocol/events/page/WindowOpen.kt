@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.page

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String
import kotlin.collections.List

/**
 * Fired when a new window is going to be opened, via window.open(), link click, form submission,
 * etc.
 */
data class WindowOpen(
  @param:JsonProperty("url")
  val url: String,
  @param:JsonProperty("windowName")
  val windowName: String,
  @param:JsonProperty("windowFeatures")
  val windowFeatures: List<String>,
  @param:JsonProperty("userGesture")
  val userGesture: Boolean,
)
