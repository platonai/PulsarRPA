@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.audits

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Information about the frame affected by an inspector issue.
 */
data class AffectedFrame(
  @param:JsonProperty("frameId")
  val frameId: String,
)
