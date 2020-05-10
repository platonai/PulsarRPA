package com.github.kklisura.cdt.protocol.types.domsnapshot;

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

import java.util.List;

public class Snapshot {

  private List<DOMNode> domNodes;

  private List<LayoutTreeNode> layoutTreeNodes;

  private List<ComputedStyle> computedStyles;

  /** The nodes in the DOM tree. The DOMNode at index 0 corresponds to the root document. */
  public List<DOMNode> getDomNodes() {
    return domNodes;
  }

  /** The nodes in the DOM tree. The DOMNode at index 0 corresponds to the root document. */
  public void setDomNodes(List<DOMNode> domNodes) {
    this.domNodes = domNodes;
  }

  /** The nodes in the layout tree. */
  public List<LayoutTreeNode> getLayoutTreeNodes() {
    return layoutTreeNodes;
  }

  /** The nodes in the layout tree. */
  public void setLayoutTreeNodes(List<LayoutTreeNode> layoutTreeNodes) {
    this.layoutTreeNodes = layoutTreeNodes;
  }

  /** Whitelisted ComputedStyle properties for each node in the layout tree. */
  public List<ComputedStyle> getComputedStyles() {
    return computedStyles;
  }

  /** Whitelisted ComputedStyle properties for each node in the layout tree. */
  public void setComputedStyles(List<ComputedStyle> computedStyles) {
    this.computedStyles = computedStyles;
  }
}
