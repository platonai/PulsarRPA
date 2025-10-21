package ai.platon.cdt.kt.protocol.events.runtime

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Issued when unhandled exception was revoked.
 */
public data class ExceptionRevoked(
  @JsonProperty("reason")
  public val reason: String,
  @JsonProperty("exceptionId")
  public val exceptionId: Int,
)
