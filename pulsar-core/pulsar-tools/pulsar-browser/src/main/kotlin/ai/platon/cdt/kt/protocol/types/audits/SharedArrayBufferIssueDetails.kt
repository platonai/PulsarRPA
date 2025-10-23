@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.audits

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean

/**
 * Details for a issue arising from an SAB being instantiated in, or
 * transferred to a context that is not cross-origin isolated.
 */
data class SharedArrayBufferIssueDetails(
  @param:JsonProperty("sourceCodeLocation")
  val sourceCodeLocation: SourceCodeLocation,
  @param:JsonProperty("isWarning")
  val isWarning: Boolean,
  @param:JsonProperty("type")
  val type: SharedArrayBufferIssueType,
)
