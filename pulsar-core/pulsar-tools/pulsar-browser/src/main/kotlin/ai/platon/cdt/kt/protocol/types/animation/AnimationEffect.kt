package ai.platon.cdt.kt.protocol.types.animation

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.Int
import kotlin.String

/**
 * AnimationEffect instance
 */
public data class AnimationEffect(
  @JsonProperty("delay")
  public val delay: Double,
  @JsonProperty("endDelay")
  public val endDelay: Double,
  @JsonProperty("iterationStart")
  public val iterationStart: Double,
  @JsonProperty("iterations")
  public val iterations: Double,
  @JsonProperty("duration")
  public val duration: Double,
  @JsonProperty("direction")
  public val direction: String,
  @JsonProperty("fill")
  public val fill: String,
  @JsonProperty("backendNodeId")
  @Optional
  public val backendNodeId: Int? = null,
  @JsonProperty("keyframesRule")
  @Optional
  public val keyframesRule: KeyframesRule? = null,
  @JsonProperty("easing")
  public val easing: String,
)
