package ai.platon.cdt.kt.protocol.events.runtime

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

/**
 * Issued when execution context is destroyed.
 */
public data class ExecutionContextDestroyed(
  @JsonProperty("executionContextId")
  public val executionContextId: Int,
)
