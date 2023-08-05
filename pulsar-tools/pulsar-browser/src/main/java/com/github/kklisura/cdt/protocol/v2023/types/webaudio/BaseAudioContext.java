package com.github.kklisura.cdt.protocol.v2023.types.webaudio;

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

/** Protocol object for BaseAudioContext */
public class BaseAudioContext {

  private String contextId;

  private ContextType contextType;

  private ContextState contextState;

  @Optional
  private ContextRealtimeData realtimeData;

  private Double callbackBufferSize;

  private Double maxOutputChannelCount;

  private Double sampleRate;

  public String getContextId() {
    return contextId;
  }

  public void setContextId(String contextId) {
    this.contextId = contextId;
  }

  public ContextType getContextType() {
    return contextType;
  }

  public void setContextType(ContextType contextType) {
    this.contextType = contextType;
  }

  public ContextState getContextState() {
    return contextState;
  }

  public void setContextState(ContextState contextState) {
    this.contextState = contextState;
  }

  public ContextRealtimeData getRealtimeData() {
    return realtimeData;
  }

  public void setRealtimeData(ContextRealtimeData realtimeData) {
    this.realtimeData = realtimeData;
  }

  /** Platform-dependent callback buffer size. */
  public Double getCallbackBufferSize() {
    return callbackBufferSize;
  }

  /** Platform-dependent callback buffer size. */
  public void setCallbackBufferSize(Double callbackBufferSize) {
    this.callbackBufferSize = callbackBufferSize;
  }

  /** Number of output channels supported by audio hardware in use. */
  public Double getMaxOutputChannelCount() {
    return maxOutputChannelCount;
  }

  /** Number of output channels supported by audio hardware in use. */
  public void setMaxOutputChannelCount(Double maxOutputChannelCount) {
    this.maxOutputChannelCount = maxOutputChannelCount;
  }

  /** Context sample rate. */
  public Double getSampleRate() {
    return sampleRate;
  }

  /** Context sample rate. */
  public void setSampleRate(Double sampleRate) {
    this.sampleRate = sampleRate;
  }
}
