@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.css

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

/**
 * Text range within a resource. All numbers are zero-based.
 */
data class SourceRange(
  @param:JsonProperty("startLine")
  val startLine: Int,
  @param:JsonProperty("startColumn")
  val startColumn: Int,
  @param:JsonProperty("endLine")
  val endLine: Int,
  @param:JsonProperty("endColumn")
  val endColumn: Int,
)
