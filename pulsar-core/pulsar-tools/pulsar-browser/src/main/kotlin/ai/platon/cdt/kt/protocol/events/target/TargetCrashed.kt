@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.target

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Issued when a target has crashed.
 */
data class TargetCrashed(
  @param:JsonProperty("targetId")
  val targetId: String,
  @param:JsonProperty("status")
  val status: String,
  @param:JsonProperty("errorCode")
  val errorCode: Int,
)
