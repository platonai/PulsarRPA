@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.inspector

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Fired when remote debugging connection is about to be terminated. Contains detach reason.
 */
data class Detached(
  @param:JsonProperty("reason")
  val reason: String,
)
