package ai.platon.cdt.kt.protocol.types.animation

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Double
import kotlin.String

/**
 * Animation instance.
 */
public data class Animation(
  @JsonProperty("id")
  public val id: String,
  @JsonProperty("name")
  public val name: String,
  @JsonProperty("pausedState")
  public val pausedState: Boolean,
  @JsonProperty("playState")
  public val playState: String,
  @JsonProperty("playbackRate")
  public val playbackRate: Double,
  @JsonProperty("startTime")
  public val startTime: Double,
  @JsonProperty("currentTime")
  public val currentTime: Double,
  @JsonProperty("type")
  public val type: AnimationType,
  @JsonProperty("source")
  @Optional
  public val source: AnimationEffect? = null,
  @JsonProperty("cssId")
  @Optional
  public val cssId: String? = null,
)
