package com.github.kklisura.cdt.protocol.types.webaudio;

/*-
 * #%L
 * cdt-java-client
 * %%
 * Copyright (C) 2018 - 2019 Kenan Klisura
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

/** Fields in AudioContext that change in real-time. */
public class ContextRealtimeData {

  private Double currentTime;

  private Double renderCapacity;

  private Double callbackIntervalMean;

  private Double callbackIntervalVariance;

  /** The current context time in second in BaseAudioContext. */
  public Double getCurrentTime() {
    return currentTime;
  }

  /** The current context time in second in BaseAudioContext. */
  public void setCurrentTime(Double currentTime) {
    this.currentTime = currentTime;
  }

  /**
   * The time spent on rendering graph divided by render qunatum duration, and multiplied by 100.
   * 100 means the audio renderer reached the full capacity and glitch may occur.
   */
  public Double getRenderCapacity() {
    return renderCapacity;
  }

  /**
   * The time spent on rendering graph divided by render qunatum duration, and multiplied by 100.
   * 100 means the audio renderer reached the full capacity and glitch may occur.
   */
  public void setRenderCapacity(Double renderCapacity) {
    this.renderCapacity = renderCapacity;
  }

  /** A running mean of callback interval. */
  public Double getCallbackIntervalMean() {
    return callbackIntervalMean;
  }

  /** A running mean of callback interval. */
  public void setCallbackIntervalMean(Double callbackIntervalMean) {
    this.callbackIntervalMean = callbackIntervalMean;
  }

  /** A running variance of callback interval. */
  public Double getCallbackIntervalVariance() {
    return callbackIntervalVariance;
  }

  /** A running variance of callback interval. */
  public void setCallbackIntervalVariance(Double callbackIntervalVariance) {
    this.callbackIntervalVariance = callbackIntervalVariance;
  }
}
