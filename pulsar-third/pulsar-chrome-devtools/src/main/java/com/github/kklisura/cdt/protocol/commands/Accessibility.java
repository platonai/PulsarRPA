package com.github.kklisura.cdt.protocol.commands;

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
import com.github.kklisura.cdt.protocol.support.annotations.Optional;
import com.github.kklisura.cdt.protocol.support.annotations.ParamName;
import com.github.kklisura.cdt.protocol.support.annotations.ReturnTypeParameter;
import com.github.kklisura.cdt.protocol.support.annotations.Returns;
import com.github.kklisura.cdt.protocol.types.accessibility.AXNode;
import java.util.List;

@Experimental
public interface Accessibility {

  /** Disables the accessibility domain. */
  void disable();

  /**
   * Enables the accessibility domain which causes `AXNodeId`s to remain consistent between method
   * calls. This turns on accessibility for the page, which can impact performance until
   * accessibility is disabled.
   */
  void enable();

  /**
   * Fetches the accessibility node and partial accessibility tree for this DOM node, if it exists.
   */
  @Experimental
  @Returns("nodes")
  @ReturnTypeParameter(AXNode.class)
  List<AXNode> getPartialAXTree();

  /**
   * Fetches the accessibility node and partial accessibility tree for this DOM node, if it exists.
   *
   * @param nodeId Identifier of the node to get the partial accessibility tree for.
   * @param backendNodeId Identifier of the backend node to get the partial accessibility tree for.
   * @param objectId JavaScript object id of the node wrapper to get the partial accessibility tree
   *     for.
   * @param fetchRelatives Whether to fetch this nodes ancestors, siblings and children. Defaults to
   *     true.
   */
  @Experimental
  @Returns("nodes")
  @ReturnTypeParameter(AXNode.class)
  List<AXNode> getPartialAXTree(
      @Optional @ParamName("nodeId") Integer nodeId,
      @Optional @ParamName("backendNodeId") Integer backendNodeId,
      @Optional @ParamName("objectId") String objectId,
      @Optional @ParamName("fetchRelatives") Boolean fetchRelatives);

  /** Fetches the entire accessibility tree */
  @Experimental
  @Returns("nodes")
  @ReturnTypeParameter(AXNode.class)
  List<AXNode> getFullAXTree();
}
