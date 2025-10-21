package ai.platon.cdt.kt.protocol.events.console

import ai.platon.cdt.kt.protocol.types.console.ConsoleMessage
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Issued when new console message is added.
 */
public data class MessageAdded(
  @JsonProperty("message")
  public val message: ConsoleMessage,
)
