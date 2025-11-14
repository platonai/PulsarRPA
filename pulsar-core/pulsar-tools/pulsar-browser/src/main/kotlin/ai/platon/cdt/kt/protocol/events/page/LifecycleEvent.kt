@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.page

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String

/**
 * Fired for top level page lifecycle events such as navigation, load, paint, etc.
 */
data class LifecycleEvent(
  @param:JsonProperty("frameId")
  val frameId: String,
  @param:JsonProperty("loaderId")
  val loaderId: String,
  @param:JsonProperty("name")
  val name: String,
  @param:JsonProperty("timestamp")
  val timestamp: Double,
)
