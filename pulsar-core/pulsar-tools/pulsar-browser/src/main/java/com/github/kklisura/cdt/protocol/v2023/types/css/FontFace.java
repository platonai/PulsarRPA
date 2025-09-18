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

/**
 * Properties of a web font:
 * https://www.w3.org/TR/2008/REC-CSS2-20080411/fonts.html#font-descriptions and additional
 * information such as platformFontFamily and fontVariationAxes.
 */
public class FontFace {

  private String fontFamily;

  private String fontStyle;

  private String fontVariant;

  private String fontWeight;

  private String fontStretch;

  private String fontDisplay;

  private String unicodeRange;

  private String src;

  private String platformFontFamily;

  @Optional
  private List<FontVariationAxis> fontVariationAxes;

  /** The font-family. */
  public String getFontFamily() {
    return fontFamily;
  }

  /** The font-family. */
  public void setFontFamily(String fontFamily) {
    this.fontFamily = fontFamily;
  }

  /** The font-style. */
  public String getFontStyle() {
    return fontStyle;
  }

  /** The font-style. */
  public void setFontStyle(String fontStyle) {
    this.fontStyle = fontStyle;
  }

  /** The font-variant. */
  public String getFontVariant() {
    return fontVariant;
  }

  /** The font-variant. */
  public void setFontVariant(String fontVariant) {
    this.fontVariant = fontVariant;
  }

  /** The font-weight. */
  public String getFontWeight() {
    return fontWeight;
  }

  /** The font-weight. */
  public void setFontWeight(String fontWeight) {
    this.fontWeight = fontWeight;
  }

  /** The font-stretch. */
  public String getFontStretch() {
    return fontStretch;
  }

  /** The font-stretch. */
  public void setFontStretch(String fontStretch) {
    this.fontStretch = fontStretch;
  }

  /** The font-display. */
  public String getFontDisplay() {
    return fontDisplay;
  }

  /** The font-display. */
  public void setFontDisplay(String fontDisplay) {
    this.fontDisplay = fontDisplay;
  }

  /** The unicode-range. */
  public String getUnicodeRange() {
    return unicodeRange;
  }

  /** The unicode-range. */
  public void setUnicodeRange(String unicodeRange) {
    this.unicodeRange = unicodeRange;
  }

  /** The src. */
  public String getSrc() {
    return src;
  }

  /** The src. */
  public void setSrc(String src) {
    this.src = src;
  }

  /** The resolved platform font family */
  public String getPlatformFontFamily() {
    return platformFontFamily;
  }

  /** The resolved platform font family */
  public void setPlatformFontFamily(String platformFontFamily) {
    this.platformFontFamily = platformFontFamily;
  }

  /** Available variation settings (a.k.a. "axes"). */
  public List<FontVariationAxis> getFontVariationAxes() {
    return fontVariationAxes;
  }

  /** Available variation settings (a.k.a. "axes"). */
  public void setFontVariationAxes(List<FontVariationAxis> fontVariationAxes) {
    this.fontVariationAxes = fontVariationAxes;
  }
}
