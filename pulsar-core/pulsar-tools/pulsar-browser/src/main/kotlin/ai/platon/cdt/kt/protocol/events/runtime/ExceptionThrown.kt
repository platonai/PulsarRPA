package ai.platon.cdt.kt.protocol.events.runtime

import ai.platon.cdt.kt.protocol.types.runtime.ExceptionDetails
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double

/**
 * Issued when exception was thrown and unhandled.
 */
public data class ExceptionThrown(
  @JsonProperty("timestamp")
  public val timestamp: Double,
  @JsonProperty("exceptionDetails")
  public val exceptionDetails: ExceptionDetails,
)
