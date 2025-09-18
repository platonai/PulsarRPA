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

/**
 * Details for issues about documents in Quirks Mode or Limited Quirks Mode that affects page
 * layouting.
 */
public class QuirksModeIssueDetails {

  private Boolean isLimitedQuirksMode;

  private Integer documentNodeId;

  private String url;

  private String frameId;

  private String loaderId;

  /** If false, it means the document's mode is "quirks" instead of "limited-quirks". */
  public Boolean getIsLimitedQuirksMode() {
    return isLimitedQuirksMode;
  }

  /** If false, it means the document's mode is "quirks" instead of "limited-quirks". */
  public void setIsLimitedQuirksMode(Boolean isLimitedQuirksMode) {
    this.isLimitedQuirksMode = isLimitedQuirksMode;
  }

  public Integer getDocumentNodeId() {
    return documentNodeId;
  }

  public void setDocumentNodeId(Integer documentNodeId) {
    this.documentNodeId = documentNodeId;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getFrameId() {
    return frameId;
  }

  public void setFrameId(String frameId) {
    this.frameId = frameId;
  }

  public String getLoaderId() {
    return loaderId;
  }

  public void setLoaderId(String loaderId) {
    this.loaderId = loaderId;
  }
}
