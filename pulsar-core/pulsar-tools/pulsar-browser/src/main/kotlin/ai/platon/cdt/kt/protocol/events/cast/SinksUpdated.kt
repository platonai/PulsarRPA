package ai.platon.cdt.kt.protocol.events.cast

import ai.platon.cdt.kt.protocol.types.cast.Sink
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.collections.List

/**
 * This is fired whenever the list of available sinks changes. A sink is a
 * device or a software surface that you can cast to.
 */
public data class SinksUpdated(
  @JsonProperty("sinks")
  public val sinks: List<Sink>,
)
