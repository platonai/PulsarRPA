@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.runtime

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

/**
 * Issued when execution context is destroyed.
 */
data class ExecutionContextDestroyed(
  @param:JsonProperty("executionContextId")
  val executionContextId: Int,
)
