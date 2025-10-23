@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.profiler

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

/**
 * Coverage data for a JavaScript script.
 */
data class ScriptCoverage(
  @param:JsonProperty("scriptId")
  val scriptId: String,
  @param:JsonProperty("url")
  val url: String,
  @param:JsonProperty("functions")
  val functions: List<FunctionCoverage>,
)
