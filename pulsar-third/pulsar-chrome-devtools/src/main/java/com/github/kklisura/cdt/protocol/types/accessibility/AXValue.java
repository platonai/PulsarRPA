package com.github.kklisura.cdt.protocol.types.accessibility;

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

import com.github.kklisura.cdt.protocol.support.annotations.Optional;
import java.util.List;

/** A single computed AX property. */
public class AXValue {

  private AXValueType type;

  @Optional private Object value;

  @Optional private List<AXRelatedNode> relatedNodes;

  @Optional private List<AXValueSource> sources;

  /** The type of this value. */
  public AXValueType getType() {
    return type;
  }

  /** The type of this value. */
  public void setType(AXValueType type) {
    this.type = type;
  }

  /** The computed value of this property. */
  public Object getValue() {
    return value;
  }

  /** The computed value of this property. */
  public void setValue(Object value) {
    this.value = value;
  }

  /** One or more related nodes, if applicable. */
  public List<AXRelatedNode> getRelatedNodes() {
    return relatedNodes;
  }

  /** One or more related nodes, if applicable. */
  public void setRelatedNodes(List<AXRelatedNode> relatedNodes) {
    this.relatedNodes = relatedNodes;
  }

  /** The sources which contributed to the computation of this property. */
  public List<AXValueSource> getSources() {
    return sources;
  }

  /** The sources which contributed to the computation of this property. */
  public void setSources(List<AXValueSource> sources) {
    this.sources = sources;
  }
}
