package ai.platon.cdt.kt.protocol.events.log

import ai.platon.cdt.kt.protocol.types.log.LogEntry
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Issued when new message was logged.
 */
public data class EntryAdded(
  @JsonProperty("entry")
  public val entry: LogEntry,
)
