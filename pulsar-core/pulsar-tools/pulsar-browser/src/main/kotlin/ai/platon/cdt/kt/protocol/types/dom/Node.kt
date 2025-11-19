@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.dom

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.Int
import kotlin.String
import kotlin.collections.List

/**
 * DOM interaction is implemented in terms of mirror objects that represent the actual DOM nodes.
 * DOMNode is a base node mirror type.
 */
data class Node(
  @param:JsonProperty("nodeId")
  val nodeId: Int,
  @param:JsonProperty("parentId")
  @param:Optional
  val parentId: Int? = null,
  @param:JsonProperty("backendNodeId")
  val backendNodeId: Int,
  @param:JsonProperty("nodeType")
  val nodeType: Int,
  @param:JsonProperty("nodeName")
  val nodeName: String,
  @param:JsonProperty("localName")
  val localName: String,
  @param:JsonProperty("nodeValue")
  val nodeValue: String,
  @param:JsonProperty("childNodeCount")
  @param:Optional
  val childNodeCount: Int? = null,
  @param:JsonProperty("children")
  @param:Optional
  val children: List<Node>? = null,
  @param:JsonProperty("attributes")
  @param:Optional
  val attributes: List<String>? = null,
  @param:JsonProperty("documentURL")
  @param:Optional
  val documentURL: String? = null,
  @param:JsonProperty("baseURL")
  @param:Optional
  val baseURL: String? = null,
  @param:JsonProperty("publicId")
  @param:Optional
  val publicId: String? = null,
  @param:JsonProperty("systemId")
  @param:Optional
  val systemId: String? = null,
  @param:JsonProperty("internalSubset")
  @param:Optional
  val internalSubset: String? = null,
  @param:JsonProperty("xmlVersion")
  @param:Optional
  val xmlVersion: String? = null,
  @param:JsonProperty("name")
  @param:Optional
  val name: String? = null,
  @param:JsonProperty("value")
  @param:Optional
  val `value`: String? = null,
  @param:JsonProperty("pseudoType")
  @param:Optional
  val pseudoType: PseudoType? = null,
  @param:JsonProperty("shadowRootType")
  @param:Optional
  val shadowRootType: ShadowRootType? = null,
  @param:JsonProperty("frameId")
  @param:Optional
  val frameId: String? = null,
  @param:JsonProperty("contentDocument")
  @param:Optional
  val contentDocument: Node? = null,
  @param:JsonProperty("shadowRoots")
  @param:Optional
  val shadowRoots: List<Node>? = null,
  @param:JsonProperty("templateContent")
  @param:Optional
  val templateContent: Node? = null,
  @param:JsonProperty("pseudoElements")
  @param:Optional
  val pseudoElements: List<Node>? = null,
  @param:JsonProperty("importedDocument")
  @param:Optional
  @Deprecated("Deprecated by protocol")
  val importedDocument: Node? = null,
  @param:JsonProperty("distributedNodes")
  @param:Optional
  val distributedNodes: List<BackendNode>? = null,
  @param:JsonProperty("isSVG")
  @param:Optional
  val isSVG: Boolean? = null,
)
