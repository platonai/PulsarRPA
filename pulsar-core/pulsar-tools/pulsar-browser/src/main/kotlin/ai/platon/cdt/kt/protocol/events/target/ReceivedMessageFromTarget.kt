@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.target

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Deprecated
import kotlin.String

/**
 * Notifies about a new protocol message received from the session (as reported in
 * `attachedToTarget` event).
 */
data class ReceivedMessageFromTarget(
  @param:JsonProperty("sessionId")
  val sessionId: String,
  @param:JsonProperty("message")
  val message: String,
  @param:JsonProperty("targetId")
  @param:Optional
  @Deprecated("Deprecated by protocol")
  val targetId: String? = null,
)
