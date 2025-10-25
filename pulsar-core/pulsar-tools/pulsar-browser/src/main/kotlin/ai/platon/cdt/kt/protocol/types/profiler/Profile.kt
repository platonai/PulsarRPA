@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.profiler

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.Int
import kotlin.collections.List

/**
 * Profile.
 */
data class Profile(
  @param:JsonProperty("nodes")
  val nodes: List<ProfileNode>,
  @param:JsonProperty("startTime")
  val startTime: Double,
  @param:JsonProperty("endTime")
  val endTime: Double,
  @param:JsonProperty("samples")
  @param:Optional
  val samples: List<Int>? = null,
  @param:JsonProperty("timeDeltas")
  @param:Optional
  val timeDeltas: List<Int>? = null,
)
