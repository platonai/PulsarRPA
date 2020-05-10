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

/** Called when a pseudo element is removed from an element. */
@Experimental
public class PseudoElementRemoved {

  private Integer parentId;

  private Integer pseudoElementId;

  /** Pseudo element's parent element id. */
  public Integer getParentId() {
    return parentId;
  }

  /** Pseudo element's parent element id. */
  public void setParentId(Integer parentId) {
    this.parentId = parentId;
  }

  /** The removed pseudo element id. */
  public Integer getPseudoElementId() {
    return pseudoElementId;
  }

  /** The removed pseudo element id. */
  public void setPseudoElementId(Integer pseudoElementId) {
    this.pseudoElementId = pseudoElementId;
  }
}
