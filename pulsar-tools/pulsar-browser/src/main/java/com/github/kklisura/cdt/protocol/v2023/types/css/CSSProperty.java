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

import com.github.kklisura.cdt.protocol.v2023.support.annotations.Experimental;
import com.github.kklisura.cdt.protocol.v2023.support.annotations.Optional;

import java.util.List;

/** CSS property declaration data. */
public class CSSProperty {

  private String name;

  private String value;

  @Optional
  private Boolean important;

  @Optional private Boolean implicit;

  @Optional private String text;

  @Optional private Boolean parsedOk;

  @Optional private Boolean disabled;

  @Optional private SourceRange range;

  @Experimental
  @Optional private List<CSSProperty> longhandProperties;

  /** The property name. */
  public String getName() {
    return name;
  }

  /** The property name. */
  public void setName(String name) {
    this.name = name;
  }

  /** The property value. */
  public String getValue() {
    return value;
  }

  /** The property value. */
  public void setValue(String value) {
    this.value = value;
  }

  /** Whether the property has "!important" annotation (implies `false` if absent). */
  public Boolean getImportant() {
    return important;
  }

  /** Whether the property has "!important" annotation (implies `false` if absent). */
  public void setImportant(Boolean important) {
    this.important = important;
  }

  /** Whether the property is implicit (implies `false` if absent). */
  public Boolean getImplicit() {
    return implicit;
  }

  /** Whether the property is implicit (implies `false` if absent). */
  public void setImplicit(Boolean implicit) {
    this.implicit = implicit;
  }

  /** The full property text as specified in the style. */
  public String getText() {
    return text;
  }

  /** The full property text as specified in the style. */
  public void setText(String text) {
    this.text = text;
  }

  /** Whether the property is understood by the browser (implies `true` if absent). */
  public Boolean getParsedOk() {
    return parsedOk;
  }

  /** Whether the property is understood by the browser (implies `true` if absent). */
  public void setParsedOk(Boolean parsedOk) {
    this.parsedOk = parsedOk;
  }

  /** Whether the property is disabled by the user (present for source-based properties only). */
  public Boolean getDisabled() {
    return disabled;
  }

  /** Whether the property is disabled by the user (present for source-based properties only). */
  public void setDisabled(Boolean disabled) {
    this.disabled = disabled;
  }

  /** The entire property range in the enclosing style declaration (if available). */
  public SourceRange getRange() {
    return range;
  }

  /** The entire property range in the enclosing style declaration (if available). */
  public void setRange(SourceRange range) {
    this.range = range;
  }

  /**
   * Parsed longhand components of this property if it is a shorthand. This field will be empty if
   * the given property is not a shorthand.
   */
  public List<CSSProperty> getLonghandProperties() {
    return longhandProperties;
  }

  /**
   * Parsed longhand components of this property if it is a shorthand. This field will be empty if
   * the given property is not a shorthand.
   */
  public void setLonghandProperties(List<CSSProperty> longhandProperties) {
    this.longhandProperties = longhandProperties;
  }
}
