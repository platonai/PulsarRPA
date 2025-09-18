package com.github.kklisura.cdt.protocol.v2023.types.accessibility;

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

public class AXRelatedNode {

  private Integer backendDOMNodeId;

  @Optional
  private String idref;

  @Optional private String text;

  /** The BackendNodeId of the related DOM node. */
  public Integer getBackendDOMNodeId() {
    return backendDOMNodeId;
  }

  /** The BackendNodeId of the related DOM node. */
  public void setBackendDOMNodeId(Integer backendDOMNodeId) {
    this.backendDOMNodeId = backendDOMNodeId;
  }

  /** The IDRef value provided, if any. */
  public String getIdref() {
    return idref;
  }

  /** The IDRef value provided, if any. */
  public void setIdref(String idref) {
    this.idref = idref;
  }

  /** The text alternative of this node in the current context. */
  public String getText() {
    return text;
  }

  /** The text alternative of this node in the current context. */
  public void setText(String text) {
    this.text = text;
  }
}
