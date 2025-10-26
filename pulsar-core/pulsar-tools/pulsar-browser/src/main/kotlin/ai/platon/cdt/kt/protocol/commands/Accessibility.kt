@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.annotations.ReturnTypeParameter
import ai.platon.cdt.kt.protocol.support.annotations.Returns
import ai.platon.cdt.kt.protocol.types.accessibility.AXNode
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.collections.List

@Experimental
interface Accessibility {
  /**
   * Disables the accessibility domain.
   */
  suspend fun disable()

  /**
   * Enables the accessibility domain which causes `AXNodeId`s to remain consistent between method calls.
   * This turns on accessibility for the page, which can impact performance until accessibility is disabled.
   */
  suspend fun enable()

  /**
   * Fetches the accessibility node and partial accessibility tree for this DOM node, if it exists.
   * @param nodeId Identifier of the node to get the partial accessibility tree for.
   * @param backendNodeId Identifier of the backend node to get the partial accessibility tree for.
   * @param objectId JavaScript object id of the node wrapper to get the partial accessibility tree for.
   * @param fetchRelatives Whether to fetch this nodes ancestors, siblings and children. Defaults to true.
   */
  @Experimental
  @Returns("nodes")
  @ReturnTypeParameter(AXNode::class)
  suspend fun getPartialAXTree(
    @ParamName("nodeId") @Optional nodeId: Int? = null,
    @ParamName("backendNodeId") @Optional backendNodeId: Int? = null,
    @ParamName("objectId") @Optional objectId: String? = null,
    @ParamName("fetchRelatives") @Optional fetchRelatives: Boolean? = null,
  ): List<AXNode>

  @Experimental
  @Returns("nodes")
  @ReturnTypeParameter(AXNode::class)
  suspend fun getPartialAXTree(): List<AXNode> {
    return getPartialAXTree(null, null, null, null)
  }

  /**
   * Fetches the entire accessibility tree for the root Document
   * @param max_depth The maximum depth at which descendants of the root node should be retrieved.
   * If omitted, the full tree is returned.
   */
  @Experimental
  @Returns("nodes")
  @ReturnTypeParameter(AXNode::class)
  suspend fun getFullAXTree(@ParamName("max_depth") @Optional max_depth: Int? = null): List<AXNode>

  /**
   * Fetches a particular accessibility node by AXNodeId.
   * Requires `enable()` to have been called previously.
   * @param id
   */
  @Experimental
  @Returns("nodes")
  @ReturnTypeParameter(AXNode::class)
  suspend fun getChildAXNodes(@ParamName("id") id: String): List<AXNode>

  /**
   * Query a DOM node's accessibility subtree for accessible name and role.
   * This command computes the name and role for all nodes in the subtree, including those that are
   * ignored for accessibility, and returns those that mactch the specified name and role. If no DOM
   * node is specified, or the DOM node does not exist, the command returns an error. If neither
   * `accessibleName` or `role` is specified, it returns all the accessibility nodes in the subtree.
   * @param nodeId Identifier of the node for the root to query.
   * @param backendNodeId Identifier of the backend node for the root to query.
   * @param objectId JavaScript object id of the node wrapper for the root to query.
   * @param accessibleName Find nodes with this computed name.
   * @param role Find nodes with this computed role.
   */
  @Experimental
  @Returns("nodes")
  @ReturnTypeParameter(AXNode::class)
  suspend fun queryAXTree(
    @ParamName("nodeId") @Optional nodeId: Int? = null,
    @ParamName("backendNodeId") @Optional backendNodeId: Int? = null,
    @ParamName("objectId") @Optional objectId: String? = null,
    @ParamName("accessibleName") @Optional accessibleName: String? = null,
    @ParamName("role") @Optional role: String? = null,
  ): List<AXNode>

  @Experimental
  @Returns("nodes")
  @ReturnTypeParameter(AXNode::class)
  suspend fun queryAXTree(): List<AXNode> {
    return queryAXTree(null, null, null, null, null)
  }
}
