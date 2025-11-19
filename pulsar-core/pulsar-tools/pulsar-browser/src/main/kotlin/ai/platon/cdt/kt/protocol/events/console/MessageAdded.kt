@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.console

import ai.platon.cdt.kt.protocol.types.console.ConsoleMessage
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Issued when new console message is added.
 */
data class MessageAdded(
  @param:JsonProperty("message")
  val message: ConsoleMessage,
)
