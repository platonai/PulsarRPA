@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.runtime

import ai.platon.cdt.kt.protocol.types.runtime.ExecutionContextDescription
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Issued when new execution context is created.
 */
data class ExecutionContextCreated(
  @param:JsonProperty("context")
  val context: ExecutionContextDescription,
)
