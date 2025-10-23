@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.target

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Deprecated
import kotlin.String

/**
 * Issued when detached from target for any reason (including `detachFromTarget` command). Can be
 * issued multiple times per target if multiple sessions have been attached to it.
 */
@Experimental
data class DetachedFromTarget(
  @param:JsonProperty("sessionId")
  val sessionId: String,
  @param:JsonProperty("targetId")
  @param:Optional
  @Deprecated("Deprecated by protocol")
  val targetId: String? = null,
)
