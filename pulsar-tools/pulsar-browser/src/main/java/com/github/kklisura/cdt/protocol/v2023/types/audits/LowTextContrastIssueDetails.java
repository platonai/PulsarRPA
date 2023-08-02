package com.github.kklisura.cdt.protocol.v2023.types.audits;

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

public class LowTextContrastIssueDetails {

  private Integer violatingNodeId;

  private String violatingNodeSelector;

  private Double contrastRatio;

  private Double thresholdAA;

  private Double thresholdAAA;

  private String fontSize;

  private String fontWeight;

  public Integer getViolatingNodeId() {
    return violatingNodeId;
  }

  public void setViolatingNodeId(Integer violatingNodeId) {
    this.violatingNodeId = violatingNodeId;
  }

  public String getViolatingNodeSelector() {
    return violatingNodeSelector;
  }

  public void setViolatingNodeSelector(String violatingNodeSelector) {
    this.violatingNodeSelector = violatingNodeSelector;
  }

  public Double getContrastRatio() {
    return contrastRatio;
  }

  public void setContrastRatio(Double contrastRatio) {
    this.contrastRatio = contrastRatio;
  }

  public Double getThresholdAA() {
    return thresholdAA;
  }

  public void setThresholdAA(Double thresholdAA) {
    this.thresholdAA = thresholdAA;
  }

  public Double getThresholdAAA() {
    return thresholdAAA;
  }

  public void setThresholdAAA(Double thresholdAAA) {
    this.thresholdAAA = thresholdAAA;
  }

  public String getFontSize() {
    return fontSize;
  }

  public void setFontSize(String fontSize) {
    this.fontSize = fontSize;
  }

  public String getFontWeight() {
    return fontWeight;
  }

  public void setFontWeight(String fontWeight) {
    this.fontWeight = fontWeight;
  }
}
