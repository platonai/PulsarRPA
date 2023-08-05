package com.github.kklisura.cdt.protocol.v2023.types.security;

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

/** Information about insecure content on the page. */
@Deprecated
public class InsecureContentStatus {

  private Boolean ranMixedContent;

  private Boolean displayedMixedContent;

  private Boolean containedMixedForm;

  private Boolean ranContentWithCertErrors;

  private Boolean displayedContentWithCertErrors;

  private SecurityState ranInsecureContentStyle;

  private SecurityState displayedInsecureContentStyle;

  /** Always false. */
  public Boolean getRanMixedContent() {
    return ranMixedContent;
  }

  /** Always false. */
  public void setRanMixedContent(Boolean ranMixedContent) {
    this.ranMixedContent = ranMixedContent;
  }

  /** Always false. */
  public Boolean getDisplayedMixedContent() {
    return displayedMixedContent;
  }

  /** Always false. */
  public void setDisplayedMixedContent(Boolean displayedMixedContent) {
    this.displayedMixedContent = displayedMixedContent;
  }

  /** Always false. */
  public Boolean getContainedMixedForm() {
    return containedMixedForm;
  }

  /** Always false. */
  public void setContainedMixedForm(Boolean containedMixedForm) {
    this.containedMixedForm = containedMixedForm;
  }

  /** Always false. */
  public Boolean getRanContentWithCertErrors() {
    return ranContentWithCertErrors;
  }

  /** Always false. */
  public void setRanContentWithCertErrors(Boolean ranContentWithCertErrors) {
    this.ranContentWithCertErrors = ranContentWithCertErrors;
  }

  /** Always false. */
  public Boolean getDisplayedContentWithCertErrors() {
    return displayedContentWithCertErrors;
  }

  /** Always false. */
  public void setDisplayedContentWithCertErrors(Boolean displayedContentWithCertErrors) {
    this.displayedContentWithCertErrors = displayedContentWithCertErrors;
  }

  /** Always set to unknown. */
  public SecurityState getRanInsecureContentStyle() {
    return ranInsecureContentStyle;
  }

  /** Always set to unknown. */
  public void setRanInsecureContentStyle(SecurityState ranInsecureContentStyle) {
    this.ranInsecureContentStyle = ranInsecureContentStyle;
  }

  /** Always set to unknown. */
  public SecurityState getDisplayedInsecureContentStyle() {
    return displayedInsecureContentStyle;
  }

  /** Always set to unknown. */
  public void setDisplayedInsecureContentStyle(SecurityState displayedInsecureContentStyle) {
    this.displayedInsecureContentStyle = displayedInsecureContentStyle;
  }
}
