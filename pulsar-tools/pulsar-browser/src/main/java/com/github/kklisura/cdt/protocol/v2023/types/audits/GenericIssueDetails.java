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

import com.github.kklisura.cdt.protocol.v2023.support.annotations.Optional;

/** Depending on the concrete errorType, different properties are set. */
public class GenericIssueDetails {

  private GenericIssueErrorType errorType;

  @Optional
  private String frameId;

  @Optional private Integer violatingNodeId;

  @Optional private String violatingNodeAttribute;

  @Optional private AffectedRequest request;

  /** Issues with the same errorType are aggregated in the frontend. */
  public GenericIssueErrorType getErrorType() {
    return errorType;
  }

  /** Issues with the same errorType are aggregated in the frontend. */
  public void setErrorType(GenericIssueErrorType errorType) {
    this.errorType = errorType;
  }

  public String getFrameId() {
    return frameId;
  }

  public void setFrameId(String frameId) {
    this.frameId = frameId;
  }

  public Integer getViolatingNodeId() {
    return violatingNodeId;
  }

  public void setViolatingNodeId(Integer violatingNodeId) {
    this.violatingNodeId = violatingNodeId;
  }

  public String getViolatingNodeAttribute() {
    return violatingNodeAttribute;
  }

  public void setViolatingNodeAttribute(String violatingNodeAttribute) {
    this.violatingNodeAttribute = violatingNodeAttribute;
  }

  public AffectedRequest getRequest() {
    return request;
  }

  public void setRequest(AffectedRequest request) {
    this.request = request;
  }
}
