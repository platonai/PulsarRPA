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
import com.github.kklisura.cdt.protocol.v2023.types.dom.LogicalAxes;
import com.github.kklisura.cdt.protocol.v2023.types.dom.PhysicalAxes;

/** CSS container query rule descriptor. */
@Experimental
public class CSSContainerQuery {

  private String text;

  @Optional
  private SourceRange range;

  @Optional private String styleSheetId;

  @Optional private String name;

  @Optional private PhysicalAxes physicalAxes;

  @Optional private LogicalAxes logicalAxes;

  /** Container query text. */
  public String getText() {
    return text;
  }

  /** Container query text. */
  public void setText(String text) {
    this.text = text;
  }

  /** The associated rule header range in the enclosing stylesheet (if available). */
  public SourceRange getRange() {
    return range;
  }

  /** The associated rule header range in the enclosing stylesheet (if available). */
  public void setRange(SourceRange range) {
    this.range = range;
  }

  /** Identifier of the stylesheet containing this object (if exists). */
  public String getStyleSheetId() {
    return styleSheetId;
  }

  /** Identifier of the stylesheet containing this object (if exists). */
  public void setStyleSheetId(String styleSheetId) {
    this.styleSheetId = styleSheetId;
  }

  /** Optional name for the container. */
  public String getName() {
    return name;
  }

  /** Optional name for the container. */
  public void setName(String name) {
    this.name = name;
  }

  /** Optional physical axes queried for the container. */
  public PhysicalAxes getPhysicalAxes() {
    return physicalAxes;
  }

  /** Optional physical axes queried for the container. */
  public void setPhysicalAxes(PhysicalAxes physicalAxes) {
    this.physicalAxes = physicalAxes;
  }

  /** Optional logical axes queried for the container. */
  public LogicalAxes getLogicalAxes() {
    return logicalAxes;
  }

  /** Optional logical axes queried for the container. */
  public void setLogicalAxes(LogicalAxes logicalAxes) {
    this.logicalAxes = logicalAxes;
  }
}
