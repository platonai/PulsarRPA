package com.github.kklisura.cdt.protocol.events.dom;

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

/** Fired when `Element`'s attribute is modified. */
public class AttributeModified {

  private Integer nodeId;

  private String name;

  private String value;

  /** Id of the node that has changed. */
  public Integer getNodeId() {
    return nodeId;
  }

  /** Id of the node that has changed. */
  public void setNodeId(Integer nodeId) {
    this.nodeId = nodeId;
  }

  /** Attribute name. */
  public String getName() {
    return name;
  }

  /** Attribute name. */
  public void setName(String name) {
    this.name = name;
  }

  /** Attribute value. */
  public String getValue() {
    return value;
  }

  /** Attribute value. */
  public void setValue(String value) {
    this.value = value;
  }
}
