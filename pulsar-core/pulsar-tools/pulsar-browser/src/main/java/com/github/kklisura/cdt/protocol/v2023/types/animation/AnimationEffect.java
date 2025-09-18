package com.github.kklisura.cdt.protocol.v2023.types.animation;

/*-
 * #%L
 * cdt-java-client
 * %%
 * Copyright (C) 2018 - 2023 Kenan Klisura
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.github.kklisura.cdt.protocol.v2023.support.annotations.Optional;

/** AnimationEffect instance */
public class AnimationEffect {

  private Double delay;

  private Double endDelay;

  private Double iterationStart;

  private Double iterations;

  private Double duration;

  private String direction;

  private String fill;

  @Optional
  private Integer backendNodeId;

  @Optional private KeyframesRule keyframesRule;

  private String easing;

  /** `AnimationEffect`'s delay. */
  public Double getDelay() {
    return delay;
  }

  /** `AnimationEffect`'s delay. */
  public void setDelay(Double delay) {
    this.delay = delay;
  }

  /** `AnimationEffect`'s end delay. */
  public Double getEndDelay() {
    return endDelay;
  }

  /** `AnimationEffect`'s end delay. */
  public void setEndDelay(Double endDelay) {
    this.endDelay = endDelay;
  }

  /** `AnimationEffect`'s iteration start. */
  public Double getIterationStart() {
    return iterationStart;
  }

  /** `AnimationEffect`'s iteration start. */
  public void setIterationStart(Double iterationStart) {
    this.iterationStart = iterationStart;
  }

  /** `AnimationEffect`'s iterations. */
  public Double getIterations() {
    return iterations;
  }

  /** `AnimationEffect`'s iterations. */
  public void setIterations(Double iterations) {
    this.iterations = iterations;
  }

  /** `AnimationEffect`'s iteration duration. */
  public Double getDuration() {
    return duration;
  }

  /** `AnimationEffect`'s iteration duration. */
  public void setDuration(Double duration) {
    this.duration = duration;
  }

  /** `AnimationEffect`'s playback direction. */
  public String getDirection() {
    return direction;
  }

  /** `AnimationEffect`'s playback direction. */
  public void setDirection(String direction) {
    this.direction = direction;
  }

  /** `AnimationEffect`'s fill mode. */
  public String getFill() {
    return fill;
  }

  /** `AnimationEffect`'s fill mode. */
  public void setFill(String fill) {
    this.fill = fill;
  }

  /** `AnimationEffect`'s target node. */
  public Integer getBackendNodeId() {
    return backendNodeId;
  }

  /** `AnimationEffect`'s target node. */
  public void setBackendNodeId(Integer backendNodeId) {
    this.backendNodeId = backendNodeId;
  }

  /** `AnimationEffect`'s keyframes. */
  public KeyframesRule getKeyframesRule() {
    return keyframesRule;
  }

  /** `AnimationEffect`'s keyframes. */
  public void setKeyframesRule(KeyframesRule keyframesRule) {
    this.keyframesRule = keyframesRule;
  }

  /** `AnimationEffect`'s timing function. */
  public String getEasing() {
    return easing;
  }

  /** `AnimationEffect`'s timing function. */
  public void setEasing(String easing) {
    this.easing = easing;
  }
}
