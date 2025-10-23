@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.log

import ai.platon.cdt.kt.protocol.types.log.LogEntry
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Issued when new message was logged.
 */
data class EntryAdded(
  @param:JsonProperty("entry")
  val entry: LogEntry,
)
