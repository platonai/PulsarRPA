package ai.platon.cdt.kt.protocol.events.tracing

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Any
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map

/**
 * Contains an bucket of collected trace events. When tracing is stopped collected events will be
 * send as a sequence of dataCollected events followed by tracingComplete event.
 */
public data class DataCollected(
  @JsonProperty("value")
  public val `value`: List<Map<String, Any?>>,
)
