package com.github.kklisura.cdt.protocol.v2023.types.preload;

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

/** Corresponds to SpeculationRuleSet */
public class RuleSet {

  private String id;

  private String loaderId;

  private String sourceText;

  @Optional
  private Integer backendNodeId;

  @Optional private String url;

  @Optional private String requestId;

  @Optional private RuleSetErrorType errorType;

  @Deprecated @Optional private String errorMessage;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  /** Identifies a document which the rule set is associated with. */
  public String getLoaderId() {
    return loaderId;
  }

  /** Identifies a document which the rule set is associated with. */
  public void setLoaderId(String loaderId) {
    this.loaderId = loaderId;
  }

  /**
   * Source text of JSON representing the rule set. If it comes from `<script>` tag, it is the
   * textContent of the node. Note that it is a JSON for valid case.
   *
   * <p>See also: - https://wicg.github.io/nav-speculation/speculation-rules.html -
   * https://github.com/WICG/nav-speculation/blob/main/triggers.md
   */
  public String getSourceText() {
    return sourceText;
  }

  /**
   * Source text of JSON representing the rule set. If it comes from `<script>` tag, it is the
   * textContent of the node. Note that it is a JSON for valid case.
   *
   * <p>See also: - https://wicg.github.io/nav-speculation/speculation-rules.html -
   * https://github.com/WICG/nav-speculation/blob/main/triggers.md
   */
  public void setSourceText(String sourceText) {
    this.sourceText = sourceText;
  }

  /**
   * A speculation rule set is either added through an inline `<script>` tag or through an external
   * resource via the 'Speculation-Rules' HTTP header. For the first case, we include the
   * BackendNodeId of the relevant `<script>` tag. For the second case, we include the external URL
   * where the rule set was loaded from, and also RequestId if Network domain is enabled.
   *
   * <p>See also: -
   * https://wicg.github.io/nav-speculation/speculation-rules.html#speculation-rules-script -
   * https://wicg.github.io/nav-speculation/speculation-rules.html#speculation-rules-header
   */
  public Integer getBackendNodeId() {
    return backendNodeId;
  }

  /**
   * A speculation rule set is either added through an inline `<script>` tag or through an external
   * resource via the 'Speculation-Rules' HTTP header. For the first case, we include the
   * BackendNodeId of the relevant `<script>` tag. For the second case, we include the external URL
   * where the rule set was loaded from, and also RequestId if Network domain is enabled.
   *
   * <p>See also: -
   * https://wicg.github.io/nav-speculation/speculation-rules.html#speculation-rules-script -
   * https://wicg.github.io/nav-speculation/speculation-rules.html#speculation-rules-header
   */
  public void setBackendNodeId(Integer backendNodeId) {
    this.backendNodeId = backendNodeId;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  /** Error information `errorMessage` is null iff `errorType` is null. */
  public RuleSetErrorType getErrorType() {
    return errorType;
  }

  /** Error information `errorMessage` is null iff `errorType` is null. */
  public void setErrorType(RuleSetErrorType errorType) {
    this.errorType = errorType;
  }

  /** TODO(https://crbug.com/1425354): Replace this property with structured error. */
  public String getErrorMessage() {
    return errorMessage;
  }

  /** TODO(https://crbug.com/1425354): Replace this property with structured error. */
  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }
}
