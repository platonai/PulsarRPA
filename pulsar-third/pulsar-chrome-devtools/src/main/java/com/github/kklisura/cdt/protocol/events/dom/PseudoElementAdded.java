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

import com.github.kklisura.cdt.protocol.support.annotations.Experimental;
import com.github.kklisura.cdt.protocol.types.dom.Node;

/** Called when a pseudo element is added to an element. */
@Experimental
public class PseudoElementAdded {

  private Integer parentId;

  private Node pseudoElement;

  /** Pseudo element's parent element id. */
  public Integer getParentId() {
    return parentId;
  }

  /** Pseudo element's parent element id. */
  public void setParentId(Integer parentId) {
    this.parentId = parentId;
  }

  /** The added pseudo element. */
  public Node getPseudoElement() {
    return pseudoElement;
  }

  /** The added pseudo element. */
  public void setPseudoElement(Node pseudoElement) {
    this.pseudoElement = pseudoElement;
  }
}
