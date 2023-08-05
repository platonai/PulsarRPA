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

import com.github.kklisura.cdt.protocol.v2023.support.annotations.Optional;

import java.util.List;

/** CSS style representation. */
public class CSSStyle {

  @Optional
  private String styleSheetId;

  private List<CSSProperty> cssProperties;

  private List<ShorthandEntry> shorthandEntries;

  @Optional private String cssText;

  @Optional private SourceRange range;

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

  /** CSS properties in the style. */
  public List<CSSProperty> getCssProperties() {
    return cssProperties;
  }

  /** CSS properties in the style. */
  public void setCssProperties(List<CSSProperty> cssProperties) {
    this.cssProperties = cssProperties;
  }

  /** Computed values for all shorthands found in the style. */
  public List<ShorthandEntry> getShorthandEntries() {
    return shorthandEntries;
  }

  /** Computed values for all shorthands found in the style. */
  public void setShorthandEntries(List<ShorthandEntry> shorthandEntries) {
    this.shorthandEntries = shorthandEntries;
  }

  /** Style declaration text (if available). */
  public String getCssText() {
    return cssText;
  }

  /** Style declaration text (if available). */
  public void setCssText(String cssText) {
    this.cssText = cssText;
  }

  /** Style declaration range in the enclosing stylesheet (if available). */
  public SourceRange getRange() {
    return range;
  }

  /** Style declaration range in the enclosing stylesheet (if available). */
  public void setRange(SourceRange range) {
    this.range = range;
  }
}
