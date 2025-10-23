@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.debugger

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Location range within one script.
 */
@Experimental
data class LocationRange(
  @param:JsonProperty("scriptId")
  val scriptId: String,
  @param:JsonProperty("start")
  val start: ScriptPosition,
  @param:JsonProperty("end")
  val end: ScriptPosition,
)
