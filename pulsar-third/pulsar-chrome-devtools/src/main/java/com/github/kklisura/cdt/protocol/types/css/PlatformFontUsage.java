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

/** Information about amount of glyphs that were rendered with given font. */
public class PlatformFontUsage {

  private String familyName;

  private Boolean isCustomFont;

  private Double glyphCount;

  /** Font's family name reported by platform. */
  public String getFamilyName() {
    return familyName;
  }

  /** Font's family name reported by platform. */
  public void setFamilyName(String familyName) {
    this.familyName = familyName;
  }

  /** Indicates if the font was downloaded or resolved locally. */
  public Boolean getIsCustomFont() {
    return isCustomFont;
  }

  /** Indicates if the font was downloaded or resolved locally. */
  public void setIsCustomFont(Boolean isCustomFont) {
    this.isCustomFont = isCustomFont;
  }

  /** Amount of glyphs that were rendered with this font. */
  public Double getGlyphCount() {
    return glyphCount;
  }

  /** Amount of glyphs that were rendered with this font. */
  public void setGlyphCount(Double glyphCount) {
    this.glyphCount = glyphCount;
  }
}
