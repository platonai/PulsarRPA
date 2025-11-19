@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.target

import ai.platon.cdt.kt.protocol.types.target.TargetInfo
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Issued when some information about a target has changed. This only happens between
 * `targetCreated` and `targetDestroyed`.
 */
data class TargetInfoChanged(
  @param:JsonProperty("targetInfo")
  val targetInfo: TargetInfo,
)
