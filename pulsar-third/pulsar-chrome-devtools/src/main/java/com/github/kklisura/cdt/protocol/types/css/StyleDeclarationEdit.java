package com.github.kklisura.cdt.protocol.types.css;

/*-
 * #%L
 * cdt-java-client
 * %%
 * Copyright (C) 2018 - 2019 Kenan Klisura
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

/** A descriptor of operation to mutate style declaration text. */
public class StyleDeclarationEdit {

  private String styleSheetId;

  private SourceRange range;

  private String text;

  /** The css style sheet identifier. */
  public String getStyleSheetId() {
    return styleSheetId;
  }

  /** The css style sheet identifier. */
  public void setStyleSheetId(String styleSheetId) {
    this.styleSheetId = styleSheetId;
  }

  /** The range of the style text in the enclosing stylesheet. */
  public SourceRange getRange() {
    return range;
  }

  /** The range of the style text in the enclosing stylesheet. */
  public void setRange(SourceRange range) {
    this.range = range;
  }

  /** New style text. */
  public String getText() {
    return text;
  }

  /** New style text. */
  public void setText(String text) {
    this.text = text;
  }
}
