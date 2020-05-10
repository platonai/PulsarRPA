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

/** A node in the accessibility tree. */
public class AXNode {

  private String nodeId;

  private Boolean ignored;

  @Optional private List<AXProperty> ignoredReasons;

  @Optional private AXValue role;

  @Optional private AXValue name;

  @Optional private AXValue description;

  @Optional private AXValue value;

  @Optional private List<AXProperty> properties;

  @Optional private List<String> childIds;

  @Optional private Integer backendDOMNodeId;

  /** Unique identifier for this node. */
  public String getNodeId() {
    return nodeId;
  }

  /** Unique identifier for this node. */
  public void setNodeId(String nodeId) {
    this.nodeId = nodeId;
  }

  /** Whether this node is ignored for accessibility */
  public Boolean getIgnored() {
    return ignored;
  }

  /** Whether this node is ignored for accessibility */
  public void setIgnored(Boolean ignored) {
    this.ignored = ignored;
  }

  /** Collection of reasons why this node is hidden. */
  public List<AXProperty> getIgnoredReasons() {
    return ignoredReasons;
  }

  /** Collection of reasons why this node is hidden. */
  public void setIgnoredReasons(List<AXProperty> ignoredReasons) {
    this.ignoredReasons = ignoredReasons;
  }

  /** This `Node`'s role, whether explicit or implicit. */
  public AXValue getRole() {
    return role;
  }

  /** This `Node`'s role, whether explicit or implicit. */
  public void setRole(AXValue role) {
    this.role = role;
  }

  /** The accessible name for this `Node`. */
  public AXValue getName() {
    return name;
  }

  /** The accessible name for this `Node`. */
  public void setName(AXValue name) {
    this.name = name;
  }

  /** The accessible description for this `Node`. */
  public AXValue getDescription() {
    return description;
  }

  /** The accessible description for this `Node`. */
  public void setDescription(AXValue description) {
    this.description = description;
  }

  /** The value for this `Node`. */
  public AXValue getValue() {
    return value;
  }

  /** The value for this `Node`. */
  public void setValue(AXValue value) {
    this.value = value;
  }

  /** All other properties */
  public List<AXProperty> getProperties() {
    return properties;
  }

  /** All other properties */
  public void setProperties(List<AXProperty> properties) {
    this.properties = properties;
  }

  /** IDs for each of this node's child nodes. */
  public List<String> getChildIds() {
    return childIds;
  }

  /** IDs for each of this node's child nodes. */
  public void setChildIds(List<String> childIds) {
    this.childIds = childIds;
  }

  /** The backend ID for the associated DOM node, if any. */
  public Integer getBackendDOMNodeId() {
    return backendDOMNodeId;
  }

  /** The backend ID for the associated DOM node, if any. */
  public void setBackendDOMNodeId(Integer backendDOMNodeId) {
    this.backendDOMNodeId = backendDOMNodeId;
  }
}
