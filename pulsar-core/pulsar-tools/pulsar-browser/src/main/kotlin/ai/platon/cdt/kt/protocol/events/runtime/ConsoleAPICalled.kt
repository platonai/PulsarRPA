@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.runtime

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.runtime.ConsoleAPICalledType
import ai.platon.cdt.kt.protocol.types.runtime.RemoteObject
import ai.platon.cdt.kt.protocol.types.runtime.StackTrace
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.Int
import kotlin.String
import kotlin.collections.List

/**
 * Issued when console API was called.
 */
data class ConsoleAPICalled(
  @param:JsonProperty("type")
  val type: ConsoleAPICalledType,
  @param:JsonProperty("args")
  val args: List<RemoteObject>,
  @param:JsonProperty("executionContextId")
  val executionContextId: Int,
  @param:JsonProperty("timestamp")
  val timestamp: Double,
  @param:JsonProperty("stackTrace")
  @param:Optional
  val stackTrace: StackTrace? = null,
  @param:JsonProperty("context")
  @param:Optional
  @param:Experimental
  val context: String? = null,
)
