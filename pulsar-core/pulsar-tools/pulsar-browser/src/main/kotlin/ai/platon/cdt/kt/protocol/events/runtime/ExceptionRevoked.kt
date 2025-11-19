@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.runtime

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Issued when unhandled exception was revoked.
 */
data class ExceptionRevoked(
  @param:JsonProperty("reason")
  val reason: String,
  @param:JsonProperty("exceptionId")
  val exceptionId: Int,
)
