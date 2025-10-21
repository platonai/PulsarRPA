package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.events.dom.AttributeModified
import ai.platon.cdt.kt.protocol.events.dom.AttributeRemoved
import ai.platon.cdt.kt.protocol.events.dom.CharacterDataModified
import ai.platon.cdt.kt.protocol.events.dom.ChildNodeCountUpdated
import ai.platon.cdt.kt.protocol.events.dom.ChildNodeInserted
import ai.platon.cdt.kt.protocol.events.dom.ChildNodeRemoved
import ai.platon.cdt.kt.protocol.events.dom.DistributedNodesUpdated
import ai.platon.cdt.kt.protocol.events.dom.DocumentUpdated
import ai.platon.cdt.kt.protocol.events.dom.InlineStyleInvalidated
import ai.platon.cdt.kt.protocol.events.dom.PseudoElementAdded
import ai.platon.cdt.kt.protocol.events.dom.PseudoElementRemoved
import ai.platon.cdt.kt.protocol.events.dom.SetChildNodes
import ai.platon.cdt.kt.protocol.events.dom.ShadowRootPopped
import ai.platon.cdt.kt.protocol.events.dom.ShadowRootPushed
import ai.platon.cdt.kt.protocol.support.annotations.EventName
import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.annotations.ReturnTypeParameter
import ai.platon.cdt.kt.protocol.support.annotations.Returns
import ai.platon.cdt.kt.protocol.support.types.EventHandler
import ai.platon.cdt.kt.protocol.support.types.EventListener
import ai.platon.cdt.kt.protocol.types.dom.BoxModel
import ai.platon.cdt.kt.protocol.types.dom.CSSComputedStyleProperty
import ai.platon.cdt.kt.protocol.types.dom.FrameOwner
import ai.platon.cdt.kt.protocol.types.dom.Node
import ai.platon.cdt.kt.protocol.types.dom.NodeForLocation
import ai.platon.cdt.kt.protocol.types.dom.PerformSearch
import ai.platon.cdt.kt.protocol.types.dom.Rect
import ai.platon.cdt.kt.protocol.types.runtime.RemoteObject
import ai.platon.cdt.kt.protocol.types.runtime.StackTrace
import java.lang.Deprecated
import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.String
import kotlin.Unit
import kotlin.collections.List

/**
 * This domain exposes DOM read/write operations. Each DOM Node is represented with its mirror
 * object
 * that has an `id`. This `id` can be used to get additional information on the Node, resolve it
 * into
 * the JavaScript object wrapper, etc. It is important that client receives DOM events only for the
 * nodes that are known to the client. Backend keeps track of the nodes that were sent to the client
 * and never sends the same node twice. It is client's responsibility to collect information about
 * the nodes that were sent to the client.<p>Note that `iframe` owner elements will return
 * corresponding document elements as their child nodes.</p>
 */
public interface DOM {
  /**
   * Collects class names for the node with given id and all of it's child nodes.
   * @param nodeId Id of the node to collect class names.
   */
  @Experimental
  @Returns("classNames")
  @ReturnTypeParameter(String::class)
  public suspend fun collectClassNamesFromSubtree(@ParamName("nodeId") nodeId: Int): List<String>

  /**
   * Creates a deep copy of the specified node and places it into the target container before the
   * given anchor.
   * @param nodeId Id of the node to copy.
   * @param targetNodeId Id of the element to drop the copy into.
   * @param insertBeforeNodeId Drop the copy before this node (if absent, the copy becomes the last
   * child of
   * `targetNodeId`).
   */
  @Experimental
  @Returns("nodeId")
  public suspend fun copyTo(
    @ParamName("nodeId") nodeId: Int,
    @ParamName("targetNodeId") targetNodeId: Int,
    @ParamName("insertBeforeNodeId") @Optional insertBeforeNodeId: Int?,
  ): Int

  @Experimental
  @Returns("nodeId")
  public suspend fun copyTo(@ParamName("nodeId") nodeId: Int, @ParamName("targetNodeId")
      targetNodeId: Int): Int {
    return copyTo(nodeId, targetNodeId, null)
  }

  /**
   * Describes node given its id, does not require domain to be enabled. Does not start tracking any
   * objects, can be used for automation.
   * @param nodeId Identifier of the node.
   * @param backendNodeId Identifier of the backend node.
   * @param objectId JavaScript object id of the node wrapper.
   * @param depth The maximum depth at which children should be retrieved, defaults to 1. Use -1 for
   * the
   * entire subtree or provide an integer larger than 0.
   * @param pierce Whether or not iframes and shadow roots should be traversed when returning the
   * subtree
   * (default is false).
   */
  @Returns("node")
  public suspend fun describeNode(
    @ParamName("nodeId") @Optional nodeId: Int?,
    @ParamName("backendNodeId") @Optional backendNodeId: Int?,
    @ParamName("objectId") @Optional objectId: String?,
    @ParamName("depth") @Optional depth: Int?,
    @ParamName("pierce") @Optional pierce: Boolean?,
  ): Node

  @Returns("node")
  public suspend fun describeNode(): Node {
    return describeNode(null, null, null, null, null)
  }

  /**
   * Scrolls the specified rect of the given node into view if not already visible.
   * Note: exactly one between nodeId, backendNodeId and objectId should be passed
   * to identify the node.
   * @param nodeId Identifier of the node.
   * @param backendNodeId Identifier of the backend node.
   * @param objectId JavaScript object id of the node wrapper.
   * @param rect The rect to be scrolled into view, relative to the node's border box, in CSS
   * pixels.
   * When omitted, center of the node will be used, similar to Element.scrollIntoView.
   */
  @Experimental
  public suspend fun scrollIntoViewIfNeeded(
    @ParamName("nodeId") @Optional nodeId: Int?,
    @ParamName("backendNodeId") @Optional backendNodeId: Int?,
    @ParamName("objectId") @Optional objectId: String?,
    @ParamName("rect") @Optional rect: Rect?,
  )

  @Experimental
  public suspend fun scrollIntoViewIfNeeded() {
    return scrollIntoViewIfNeeded(null, null, null, null)
  }

  /**
   * Disables DOM agent for the given page.
   */
  public suspend fun disable()

  /**
   * Discards search results from the session with the given id. `getSearchResults` should no longer
   * be called for that search.
   * @param searchId Unique search session identifier.
   */
  @Experimental
  public suspend fun discardSearchResults(@ParamName("searchId") searchId: String)

  /**
   * Enables DOM agent for the given page.
   */
  public suspend fun enable()

  /**
   * Focuses the given element.
   * @param nodeId Identifier of the node.
   * @param backendNodeId Identifier of the backend node.
   * @param objectId JavaScript object id of the node wrapper.
   */
  public suspend fun focus(
    @ParamName("nodeId") @Optional nodeId: Int?,
    @ParamName("backendNodeId") @Optional backendNodeId: Int?,
    @ParamName("objectId") @Optional objectId: String?,
  )

  public suspend fun focus() {
    return focus(null, null, null)
  }

  /**
   * Returns attributes for the specified node.
   * @param nodeId Id of the node to retrieve attibutes for.
   */
  @Returns("attributes")
  @ReturnTypeParameter(String::class)
  public suspend fun getAttributes(@ParamName("nodeId") nodeId: Int): List<String>

  /**
   * Returns boxes for the given node.
   * @param nodeId Identifier of the node.
   * @param backendNodeId Identifier of the backend node.
   * @param objectId JavaScript object id of the node wrapper.
   */
  @Returns("model")
  public suspend fun getBoxModel(
    @ParamName("nodeId") @Optional nodeId: Int?,
    @ParamName("backendNodeId") @Optional backendNodeId: Int?,
    @ParamName("objectId") @Optional objectId: String?,
  ): BoxModel

  @Returns("model")
  public suspend fun getBoxModel(): BoxModel {
    return getBoxModel(null, null, null)
  }

  /**
   * Returns quads that describe node position on the page. This method
   * might return multiple quads for inline nodes.
   * @param nodeId Identifier of the node.
   * @param backendNodeId Identifier of the backend node.
   * @param objectId JavaScript object id of the node wrapper.
   */
  @Experimental
  @Returns("quads")
  @ReturnTypeParameter(Double::class)
  public suspend fun getContentQuads(
    @ParamName("nodeId") @Optional nodeId: Int?,
    @ParamName("backendNodeId") @Optional backendNodeId: Int?,
    @ParamName("objectId") @Optional objectId: String?,
  ): List<List<Double>>

  @Experimental
  @Returns("quads")
  @ReturnTypeParameter(Double::class)
  public suspend fun getContentQuads(): List<List<Double>> {
    return getContentQuads(null, null, null)
  }

  /**
   * Returns the root DOM node (and optionally the subtree) to the caller.
   * @param depth The maximum depth at which children should be retrieved, defaults to 1. Use -1 for
   * the
   * entire subtree or provide an integer larger than 0.
   * @param pierce Whether or not iframes and shadow roots should be traversed when returning the
   * subtree
   * (default is false).
   */
  @Returns("root")
  public suspend fun getDocument(@ParamName("depth") @Optional depth: Int?, @ParamName("pierce")
      @Optional pierce: Boolean?): Node

  @Returns("root")
  public suspend fun getDocument(): Node {
    return getDocument(null, null)
  }

  /**
   * Returns the root DOM node (and optionally the subtree) to the caller.
   * Deprecated, as it is not designed to work well with the rest of the DOM agent.
   * Use DOMSnapshot.captureSnapshot instead.
   * @param depth The maximum depth at which children should be retrieved, defaults to 1. Use -1 for
   * the
   * entire subtree or provide an integer larger than 0.
   * @param pierce Whether or not iframes and shadow roots should be traversed when returning the
   * subtree
   * (default is false).
   */
  @Deprecated
  @Returns("nodes")
  @ReturnTypeParameter(Node::class)
  public suspend fun getFlattenedDocument(@ParamName("depth") @Optional depth: Int?,
      @ParamName("pierce") @Optional pierce: Boolean?): List<Node>

  @Deprecated
  @Returns("nodes")
  @ReturnTypeParameter(Node::class)
  public suspend fun getFlattenedDocument(): List<Node> {
    return getFlattenedDocument(null, null)
  }

  /**
   * Finds nodes with a given computed style in a subtree.
   * @param nodeId Node ID pointing to the root of a subtree.
   * @param computedStyles The style to filter nodes by (includes nodes if any of properties
   * matches).
   * @param pierce Whether or not iframes and shadow roots in the same target should be traversed
   * when returning the
   * results (default is false).
   */
  @Experimental
  @Returns("nodeIds")
  @ReturnTypeParameter(Int::class)
  public suspend fun getNodesForSubtreeByStyle(
    @ParamName("nodeId") nodeId: Int,
    @ParamName("computedStyles") computedStyles: List<CSSComputedStyleProperty>,
    @ParamName("pierce") @Optional pierce: Boolean?,
  ): List<Int>

  @Experimental
  @Returns("nodeIds")
  @ReturnTypeParameter(Int::class)
  public suspend fun getNodesForSubtreeByStyle(@ParamName("nodeId") nodeId: Int,
      @ParamName("computedStyles") computedStyles: List<CSSComputedStyleProperty>): List<Int> {
    return getNodesForSubtreeByStyle(nodeId, computedStyles, null)
  }

  /**
   * Returns node id at given location. Depending on whether DOM domain is enabled, nodeId is
   * either returned or not.
   * @param x X coordinate.
   * @param y Y coordinate.
   * @param includeUserAgentShadowDOM False to skip to the nearest non-UA shadow root ancestor
   * (default: false).
   * @param ignorePointerEventsNone Whether to ignore pointer-events: none on elements and hit test
   * them.
   */
  public suspend fun getNodeForLocation(
    @ParamName("x") x: Int,
    @ParamName("y") y: Int,
    @ParamName("includeUserAgentShadowDOM") @Optional includeUserAgentShadowDOM: Boolean?,
    @ParamName("ignorePointerEventsNone") @Optional ignorePointerEventsNone: Boolean?,
  ): NodeForLocation

  public suspend fun getNodeForLocation(@ParamName("x") x: Int, @ParamName("y") y: Int):
      NodeForLocation {
    return getNodeForLocation(x, y, null, null)
  }

  /**
   * Returns node's HTML markup.
   * @param nodeId Identifier of the node.
   * @param backendNodeId Identifier of the backend node.
   * @param objectId JavaScript object id of the node wrapper.
   */
  @Returns("outerHTML")
  public suspend fun getOuterHTML(
    @ParamName("nodeId") @Optional nodeId: Int?,
    @ParamName("backendNodeId") @Optional backendNodeId: Int?,
    @ParamName("objectId") @Optional objectId: String?,
  ): String

  @Returns("outerHTML")
  public suspend fun getOuterHTML(): String {
    return getOuterHTML(null, null, null)
  }

  /**
   * Returns the id of the nearest ancestor that is a relayout boundary.
   * @param nodeId Id of the node.
   */
  @Experimental
  @Returns("nodeId")
  public suspend fun getRelayoutBoundary(@ParamName("nodeId") nodeId: Int): Int

  /**
   * Returns search results from given `fromIndex` to given `toIndex` from the search with the given
   * identifier.
   * @param searchId Unique search session identifier.
   * @param fromIndex Start index of the search result to be returned.
   * @param toIndex End index of the search result to be returned.
   */
  @Experimental
  @Returns("nodeIds")
  @ReturnTypeParameter(Int::class)
  public suspend fun getSearchResults(
    @ParamName("searchId") searchId: String,
    @ParamName("fromIndex") fromIndex: Int,
    @ParamName("toIndex") toIndex: Int,
  ): List<Int>

  /**
   * Marks last undoable state.
   */
  @Experimental
  public suspend fun markUndoableState()

  /**
   * Moves node into the new container, places it before the given anchor.
   * @param nodeId Id of the node to move.
   * @param targetNodeId Id of the element to drop the moved node into.
   * @param insertBeforeNodeId Drop node before this one (if absent, the moved node becomes the last
   * child of
   * `targetNodeId`).
   */
  @Returns("nodeId")
  public suspend fun moveTo(
    @ParamName("nodeId") nodeId: Int,
    @ParamName("targetNodeId") targetNodeId: Int,
    @ParamName("insertBeforeNodeId") @Optional insertBeforeNodeId: Int?,
  ): Int

  @Returns("nodeId")
  public suspend fun moveTo(@ParamName("nodeId") nodeId: Int, @ParamName("targetNodeId")
      targetNodeId: Int): Int {
    return moveTo(nodeId, targetNodeId, null)
  }

  /**
   * Searches for a given string in the DOM tree. Use `getSearchResults` to access search results or
   * `cancelSearch` to end this search session.
   * @param query Plain text or query selector or XPath search query.
   * @param includeUserAgentShadowDOM True to search in user agent shadow DOM.
   */
  @Experimental
  public suspend fun performSearch(@ParamName("query") query: String,
      @ParamName("includeUserAgentShadowDOM") @Optional includeUserAgentShadowDOM: Boolean?):
      PerformSearch

  @Experimental
  public suspend fun performSearch(@ParamName("query") query: String): PerformSearch {
    return performSearch(query, null)
  }

  /**
   * Requests that the node is sent to the caller given its path. // FIXME, use XPath
   * @param path Path to node in the proprietary format.
   */
  @Experimental
  @Returns("nodeId")
  public suspend fun pushNodeByPathToFrontend(@ParamName("path") path: String): Int

  /**
   * Requests that a batch of nodes is sent to the caller given their backend node ids.
   * @param backendNodeIds The array of backend node ids.
   */
  @Experimental
  @Returns("nodeIds")
  @ReturnTypeParameter(Int::class)
  public suspend fun pushNodesByBackendIdsToFrontend(@ParamName("backendNodeIds")
      backendNodeIds: List<Int>): List<Int>

  /**
   * Executes `querySelector` on a given node.
   * @param nodeId Id of the node to query upon.
   * @param selector Selector string.
   */
  @Returns("nodeId")
  public suspend fun querySelector(@ParamName("nodeId") nodeId: Int, @ParamName("selector")
      selector: String): Int

  /**
   * Executes `querySelectorAll` on a given node.
   * @param nodeId Id of the node to query upon.
   * @param selector Selector string.
   */
  @Returns("nodeIds")
  @ReturnTypeParameter(Int::class)
  public suspend fun querySelectorAll(@ParamName("nodeId") nodeId: Int, @ParamName("selector")
      selector: String): List<Int>

  /**
   * Re-does the last undone action.
   */
  @Experimental
  public suspend fun redo()

  /**
   * Removes attribute with given name from an element with given id.
   * @param nodeId Id of the element to remove attribute from.
   * @param name Name of the attribute to remove.
   */
  public suspend fun removeAttribute(@ParamName("nodeId") nodeId: Int, @ParamName("name")
      name: String)

  /**
   * Removes node with given id.
   * @param nodeId Id of the node to remove.
   */
  public suspend fun removeNode(@ParamName("nodeId") nodeId: Int)

  /**
   * Requests that children of the node with given id are returned to the caller in form of
   * `setChildNodes` events where not only immediate children are retrieved, but all children down
   * to
   * the specified depth.
   * @param nodeId Id of the node to get children for.
   * @param depth The maximum depth at which children should be retrieved, defaults to 1. Use -1 for
   * the
   * entire subtree or provide an integer larger than 0.
   * @param pierce Whether or not iframes and shadow roots should be traversed when returning the
   * sub-tree
   * (default is false).
   */
  public suspend fun requestChildNodes(
    @ParamName("nodeId") nodeId: Int,
    @ParamName("depth") @Optional depth: Int?,
    @ParamName("pierce") @Optional pierce: Boolean?,
  )

  public suspend fun requestChildNodes(@ParamName("nodeId") nodeId: Int) {
    return requestChildNodes(nodeId, null, null)
  }

  /**
   * Requests that the node is sent to the caller given the JavaScript node object reference. All
   * nodes that form the path from the node to the root are also sent to the client as a series of
   * `setChildNodes` notifications.
   * @param objectId JavaScript object id to convert into node.
   */
  @Returns("nodeId")
  public suspend fun requestNode(@ParamName("objectId") objectId: String): Int

  /**
   * Resolves the JavaScript node object for a given NodeId or BackendNodeId.
   * @param nodeId Id of the node to resolve.
   * @param backendNodeId Backend identifier of the node to resolve.
   * @param objectGroup Symbolic group name that can be used to release multiple objects.
   * @param executionContextId Execution context in which to resolve the node.
   */
  @Returns("object")
  public suspend fun resolveNode(
    @ParamName("nodeId") @Optional nodeId: Int?,
    @ParamName("backendNodeId") @Optional backendNodeId: Int?,
    @ParamName("objectGroup") @Optional objectGroup: String?,
    @ParamName("executionContextId") @Optional executionContextId: Int?,
  ): RemoteObject

  @Returns("object")
  public suspend fun resolveNode(): RemoteObject {
    return resolveNode(null, null, null, null)
  }

  /**
   * Sets attribute for an element with given id.
   * @param nodeId Id of the element to set attribute for.
   * @param name Attribute name.
   * @param value Attribute value.
   */
  public suspend fun setAttributeValue(
    @ParamName("nodeId") nodeId: Int,
    @ParamName("name") name: String,
    @ParamName("value") `value`: String,
  )

  /**
   * Sets attributes on element with given id. This method is useful when user edits some existing
   * attribute value and types in several attribute name/value pairs.
   * @param nodeId Id of the element to set attributes for.
   * @param text Text with a number of attributes. Will parse this text using HTML parser.
   * @param name Attribute name to replace with new attributes derived from text in case text parsed
   * successfully.
   */
  public suspend fun setAttributesAsText(
    @ParamName("nodeId") nodeId: Int,
    @ParamName("text") text: String,
    @ParamName("name") @Optional name: String?,
  )

  public suspend fun setAttributesAsText(@ParamName("nodeId") nodeId: Int, @ParamName("text")
      text: String) {
    return setAttributesAsText(nodeId, text, null)
  }

  /**
   * Sets files for the given file input element.
   * @param files Array of file paths to set.
   * @param nodeId Identifier of the node.
   * @param backendNodeId Identifier of the backend node.
   * @param objectId JavaScript object id of the node wrapper.
   */
  public suspend fun setFileInputFiles(
    @ParamName("files") files: List<String>,
    @ParamName("nodeId") @Optional nodeId: Int?,
    @ParamName("backendNodeId") @Optional backendNodeId: Int?,
    @ParamName("objectId") @Optional objectId: String?,
  )

  public suspend fun setFileInputFiles(@ParamName("files") files: List<String>) {
    return setFileInputFiles(files, null, null, null)
  }

  /**
   * Sets if stack traces should be captured for Nodes. See `Node.getNodeStackTraces`. Default is
   * disabled.
   * @param enable Enable or disable.
   */
  @Experimental
  public suspend fun setNodeStackTracesEnabled(@ParamName("enable") enable: Boolean)

  /**
   * Gets stack traces associated with a Node. As of now, only provides stack trace for Node
   * creation.
   * @param nodeId Id of the node to get stack traces for.
   */
  @Experimental
  @Returns("creation")
  public suspend fun getNodeStackTraces(@ParamName("nodeId") nodeId: Int): StackTrace?

  /**
   * Returns file information for the given
   * File wrapper.
   * @param objectId JavaScript object id of the node wrapper.
   */
  @Experimental
  @Returns("path")
  public suspend fun getFileInfo(@ParamName("objectId") objectId: String): String

  /**
   * Enables console to refer to the node with given id via $x (see Command Line API for more
   * details
   * $x functions).
   * @param nodeId DOM node id to be accessible by means of $x command line API.
   */
  @Experimental
  public suspend fun setInspectedNode(@ParamName("nodeId") nodeId: Int)

  /**
   * Sets node name for a node with given id.
   * @param nodeId Id of the node to set name for.
   * @param name New node's name.
   */
  @Returns("nodeId")
  public suspend fun setNodeName(@ParamName("nodeId") nodeId: Int, @ParamName("name") name: String):
      Int

  /**
   * Sets node value for a node with given id.
   * @param nodeId Id of the node to set value for.
   * @param value New node's value.
   */
  public suspend fun setNodeValue(@ParamName("nodeId") nodeId: Int, @ParamName("value")
      `value`: String)

  /**
   * Sets node HTML markup, returns new node id.
   * @param nodeId Id of the node to set markup for.
   * @param outerHTML Outer HTML markup to set.
   */
  public suspend fun setOuterHTML(@ParamName("nodeId") nodeId: Int, @ParamName("outerHTML")
      outerHTML: String)

  /**
   * Undoes the last performed action.
   */
  @Experimental
  public suspend fun undo()

  /**
   * Returns iframe node that owns iframe with the given domain.
   * @param frameId
   */
  @Experimental
  public suspend fun getFrameOwner(@ParamName("frameId") frameId: String): FrameOwner

  @EventName("attributeModified")
  public fun onAttributeModified(eventListener: EventHandler<AttributeModified>): EventListener

  @EventName("attributeModified")
  public fun onAttributeModified(eventListener: suspend (AttributeModified) -> Unit): EventListener

  @EventName("attributeRemoved")
  public fun onAttributeRemoved(eventListener: EventHandler<AttributeRemoved>): EventListener

  @EventName("attributeRemoved")
  public fun onAttributeRemoved(eventListener: suspend (AttributeRemoved) -> Unit): EventListener

  @EventName("characterDataModified")
  public fun onCharacterDataModified(eventListener: EventHandler<CharacterDataModified>):
      EventListener

  @EventName("characterDataModified")
  public fun onCharacterDataModified(eventListener: suspend (CharacterDataModified) -> Unit):
      EventListener

  @EventName("childNodeCountUpdated")
  public fun onChildNodeCountUpdated(eventListener: EventHandler<ChildNodeCountUpdated>):
      EventListener

  @EventName("childNodeCountUpdated")
  public fun onChildNodeCountUpdated(eventListener: suspend (ChildNodeCountUpdated) -> Unit):
      EventListener

  @EventName("childNodeInserted")
  public fun onChildNodeInserted(eventListener: EventHandler<ChildNodeInserted>): EventListener

  @EventName("childNodeInserted")
  public fun onChildNodeInserted(eventListener: suspend (ChildNodeInserted) -> Unit): EventListener

  @EventName("childNodeRemoved")
  public fun onChildNodeRemoved(eventListener: EventHandler<ChildNodeRemoved>): EventListener

  @EventName("childNodeRemoved")
  public fun onChildNodeRemoved(eventListener: suspend (ChildNodeRemoved) -> Unit): EventListener

  @EventName("distributedNodesUpdated")
  @Experimental
  public fun onDistributedNodesUpdated(eventListener: EventHandler<DistributedNodesUpdated>):
      EventListener

  @EventName("distributedNodesUpdated")
  @Experimental
  public fun onDistributedNodesUpdated(eventListener: suspend (DistributedNodesUpdated) -> Unit):
      EventListener

  @EventName("documentUpdated")
  public fun onDocumentUpdated(eventListener: EventHandler<DocumentUpdated>): EventListener

  @EventName("documentUpdated")
  public fun onDocumentUpdated(eventListener: suspend (DocumentUpdated) -> Unit): EventListener

  @EventName("inlineStyleInvalidated")
  @Experimental
  public fun onInlineStyleInvalidated(eventListener: EventHandler<InlineStyleInvalidated>):
      EventListener

  @EventName("inlineStyleInvalidated")
  @Experimental
  public fun onInlineStyleInvalidated(eventListener: suspend (InlineStyleInvalidated) -> Unit):
      EventListener

  @EventName("pseudoElementAdded")
  @Experimental
  public fun onPseudoElementAdded(eventListener: EventHandler<PseudoElementAdded>): EventListener

  @EventName("pseudoElementAdded")
  @Experimental
  public fun onPseudoElementAdded(eventListener: suspend (PseudoElementAdded) -> Unit):
      EventListener

  @EventName("pseudoElementRemoved")
  @Experimental
  public fun onPseudoElementRemoved(eventListener: EventHandler<PseudoElementRemoved>):
      EventListener

  @EventName("pseudoElementRemoved")
  @Experimental
  public fun onPseudoElementRemoved(eventListener: suspend (PseudoElementRemoved) -> Unit):
      EventListener

  @EventName("setChildNodes")
  public fun onSetChildNodes(eventListener: EventHandler<SetChildNodes>): EventListener

  @EventName("setChildNodes")
  public fun onSetChildNodes(eventListener: suspend (SetChildNodes) -> Unit): EventListener

  @EventName("shadowRootPopped")
  @Experimental
  public fun onShadowRootPopped(eventListener: EventHandler<ShadowRootPopped>): EventListener

  @EventName("shadowRootPopped")
  @Experimental
  public fun onShadowRootPopped(eventListener: suspend (ShadowRootPopped) -> Unit): EventListener

  @EventName("shadowRootPushed")
  @Experimental
  public fun onShadowRootPushed(eventListener: EventHandler<ShadowRootPushed>): EventListener

  @EventName("shadowRootPushed")
  @Experimental
  public fun onShadowRootPushed(eventListener: suspend (ShadowRootPushed) -> Unit): EventListener
}
