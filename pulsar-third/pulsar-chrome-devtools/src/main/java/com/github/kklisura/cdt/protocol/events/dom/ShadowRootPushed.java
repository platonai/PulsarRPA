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

/** Called when shadow root is pushed into the element. */
@Experimental
public class ShadowRootPushed {

  private Integer hostId;

  private Node root;

  /** Host element id. */
  public Integer getHostId() {
    return hostId;
  }

  /** Host element id. */
  public void setHostId(Integer hostId) {
    this.hostId = hostId;
  }

  /** Shadow root. */
  public Node getRoot() {
    return root;
  }

  /** Shadow root. */
  public void setRoot(Node root) {
    this.root = root;
  }
}
