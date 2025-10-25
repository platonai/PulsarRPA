@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.animation

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Animation type of `Animation`.
 */
public enum class AnimationType {
  @JsonProperty("CSSTransition")
  CSS_TRANSITION,
  @JsonProperty("CSSAnimation")
  CSS_ANIMATION,
  @JsonProperty("WebAnimation")
  WEB_ANIMATION,
}
