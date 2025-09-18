package com.github.kklisura.cdt.protocol.v2023.types.css;

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

/** CSS coverage information. */
public class RuleUsage {

  private String styleSheetId;

  private Double startOffset;

  private Double endOffset;

  private Boolean used;

  /**
   * The css style sheet identifier (absent for user agent stylesheet and user-specified stylesheet
   * rules) this rule came from.
   */
  public String getStyleSheetId() {
    return styleSheetId;
  }

  /**
   * The css style sheet identifier (absent for user agent stylesheet and user-specified stylesheet
   * rules) this rule came from.
   */
  public void setStyleSheetId(String styleSheetId) {
    this.styleSheetId = styleSheetId;
  }

  /** Offset of the start of the rule (including selector) from the beginning of the stylesheet. */
  public Double getStartOffset() {
    return startOffset;
  }

  /** Offset of the start of the rule (including selector) from the beginning of the stylesheet. */
  public void setStartOffset(Double startOffset) {
    this.startOffset = startOffset;
  }

  /** Offset of the end of the rule body from the beginning of the stylesheet. */
  public Double getEndOffset() {
    return endOffset;
  }

  /** Offset of the end of the rule body from the beginning of the stylesheet. */
  public void setEndOffset(Double endOffset) {
    this.endOffset = endOffset;
  }

  /** Indicates whether the rule was actually used by some element in the page. */
  public Boolean getUsed() {
    return used;
  }

  /** Indicates whether the rule was actually used by some element in the page. */
  public void setUsed(Boolean used) {
    this.used = used;
  }
}
