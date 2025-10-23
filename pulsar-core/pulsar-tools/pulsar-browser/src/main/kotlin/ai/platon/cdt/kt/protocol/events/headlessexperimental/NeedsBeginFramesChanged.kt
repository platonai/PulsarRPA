@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.headlessexperimental

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Deprecated

/**
 * Issued when the target starts or stops needing BeginFrames.
 * Deprecated. Issue beginFrame unconditionally instead and use result from
 * beginFrame to detect whether the frames were suppressed.
 */
@Deprecated("Deprecated")
data class NeedsBeginFramesChanged(
  @param:JsonProperty("needsBeginFrames")
  val needsBeginFrames: Boolean,
)
