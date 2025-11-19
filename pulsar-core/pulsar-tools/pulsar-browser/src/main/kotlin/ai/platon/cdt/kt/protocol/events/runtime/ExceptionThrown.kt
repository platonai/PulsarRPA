@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.runtime

import ai.platon.cdt.kt.protocol.types.runtime.ExceptionDetails
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double

/**
 * Issued when exception was thrown and unhandled.
 */
data class ExceptionThrown(
  @param:JsonProperty("timestamp")
  val timestamp: Double,
  @param:JsonProperty("exceptionDetails")
  val exceptionDetails: ExceptionDetails,
)
