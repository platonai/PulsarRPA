@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.animation

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Double
import kotlin.String

/**
 * Animation instance.
 */
data class Animation(
  @param:JsonProperty("id")
  val id: String,
  @param:JsonProperty("name")
  val name: String,
  @param:JsonProperty("pausedState")
  val pausedState: Boolean,
  @param:JsonProperty("playState")
  val playState: String,
  @param:JsonProperty("playbackRate")
  val playbackRate: Double,
  @param:JsonProperty("startTime")
  val startTime: Double,
  @param:JsonProperty("currentTime")
  val currentTime: Double,
  @param:JsonProperty("type")
  val type: AnimationType,
  @param:JsonProperty("source")
  @param:Optional
  val source: AnimationEffect? = null,
  @param:JsonProperty("cssId")
  @param:Optional
  val cssId: String? = null,
)
