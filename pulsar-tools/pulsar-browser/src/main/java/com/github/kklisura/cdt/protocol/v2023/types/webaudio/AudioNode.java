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

/** Protocol object for AudioNode */
public class AudioNode {

  private String nodeId;

  private String contextId;

  private String nodeType;

  private Double numberOfInputs;

  private Double numberOfOutputs;

  private Double channelCount;

  private ChannelCountMode channelCountMode;

  private ChannelInterpretation channelInterpretation;

  public String getNodeId() {
    return nodeId;
  }

  public void setNodeId(String nodeId) {
    this.nodeId = nodeId;
  }

  public String getContextId() {
    return contextId;
  }

  public void setContextId(String contextId) {
    this.contextId = contextId;
  }

  public String getNodeType() {
    return nodeType;
  }

  public void setNodeType(String nodeType) {
    this.nodeType = nodeType;
  }

  public Double getNumberOfInputs() {
    return numberOfInputs;
  }

  public void setNumberOfInputs(Double numberOfInputs) {
    this.numberOfInputs = numberOfInputs;
  }

  public Double getNumberOfOutputs() {
    return numberOfOutputs;
  }

  public void setNumberOfOutputs(Double numberOfOutputs) {
    this.numberOfOutputs = numberOfOutputs;
  }

  public Double getChannelCount() {
    return channelCount;
  }

  public void setChannelCount(Double channelCount) {
    this.channelCount = channelCount;
  }

  public ChannelCountMode getChannelCountMode() {
    return channelCountMode;
  }

  public void setChannelCountMode(ChannelCountMode channelCountMode) {
    this.channelCountMode = channelCountMode;
  }

  public ChannelInterpretation getChannelInterpretation() {
    return channelInterpretation;
  }

  public void setChannelInterpretation(ChannelInterpretation channelInterpretation) {
    this.channelInterpretation = channelInterpretation;
  }
}
