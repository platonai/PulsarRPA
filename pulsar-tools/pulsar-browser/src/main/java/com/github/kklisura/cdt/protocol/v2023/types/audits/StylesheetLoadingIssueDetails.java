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

/** This issue warns when a referenced stylesheet couldn't be loaded. */
public class StylesheetLoadingIssueDetails {

  private SourceCodeLocation sourceCodeLocation;

  private StyleSheetLoadingIssueReason styleSheetLoadingIssueReason;

  @Optional
  private FailedRequestInfo failedRequestInfo;

  /** Source code position that referenced the failing stylesheet. */
  public SourceCodeLocation getSourceCodeLocation() {
    return sourceCodeLocation;
  }

  /** Source code position that referenced the failing stylesheet. */
  public void setSourceCodeLocation(SourceCodeLocation sourceCodeLocation) {
    this.sourceCodeLocation = sourceCodeLocation;
  }

  /** Reason why the stylesheet couldn't be loaded. */
  public StyleSheetLoadingIssueReason getStyleSheetLoadingIssueReason() {
    return styleSheetLoadingIssueReason;
  }

  /** Reason why the stylesheet couldn't be loaded. */
  public void setStyleSheetLoadingIssueReason(
      StyleSheetLoadingIssueReason styleSheetLoadingIssueReason) {
    this.styleSheetLoadingIssueReason = styleSheetLoadingIssueReason;
  }

  /** Contains additional info when the failure was due to a request. */
  public FailedRequestInfo getFailedRequestInfo() {
    return failedRequestInfo;
  }

  /** Contains additional info when the failure was due to a request. */
  public void setFailedRequestInfo(FailedRequestInfo failedRequestInfo) {
    this.failedRequestInfo = failedRequestInfo;
  }
}
