@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.target

import ai.platon.cdt.kt.protocol.types.target.TargetInfo
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Issued when a possible inspection target is created.
 */
data class TargetCreated(
  @param:JsonProperty("targetInfo")
  val targetInfo: TargetInfo,
)
