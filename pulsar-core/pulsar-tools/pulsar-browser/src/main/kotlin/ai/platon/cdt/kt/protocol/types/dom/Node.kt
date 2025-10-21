package ai.platon.cdt.kt.protocol.types.dom

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import java.lang.Deprecated
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.collections.List

/**
 * DOM interaction is implemented in terms of mirror objects that represent the actual DOM nodes.
 * DOMNode is a base node mirror type.
 */
public data class Node(
  @JsonProperty("nodeId")
  public val nodeId: Int,
  @JsonProperty("parentId")
  @Optional
  public val parentId: Int? = null,
  @JsonProperty("backendNodeId")
  public val backendNodeId: Int,
  @JsonProperty("nodeType")
  public val nodeType: Int,
  @JsonProperty("nodeName")
  public val nodeName: String,
  @JsonProperty("localName")
  public val localName: String,
  @JsonProperty("nodeValue")
  public val nodeValue: String,
  @JsonProperty("childNodeCount")
  @Optional
  public val childNodeCount: Int? = null,
  @JsonProperty("children")
  @Optional
  public val children: List<Node>? = null,
  @JsonProperty("attributes")
  @Optional
  public val attributes: List<String>? = null,
  @JsonProperty("documentURL")
  @Optional
  public val documentURL: String? = null,
  @JsonProperty("baseURL")
  @Optional
  public val baseURL: String? = null,
  @JsonProperty("publicId")
  @Optional
  public val publicId: String? = null,
  @JsonProperty("systemId")
  @Optional
  public val systemId: String? = null,
  @JsonProperty("internalSubset")
  @Optional
  public val internalSubset: String? = null,
  @JsonProperty("xmlVersion")
  @Optional
  public val xmlVersion: String? = null,
  @JsonProperty("name")
  @Optional
  public val name: String? = null,
  @JsonProperty("value")
  @Optional
  public val `value`: String? = null,
  @JsonProperty("pseudoType")
  @Optional
  public val pseudoType: PseudoType? = null,
  @JsonProperty("shadowRootType")
  @Optional
  public val shadowRootType: ShadowRootType? = null,
  @JsonProperty("frameId")
  @Optional
  public val frameId: String? = null,
  @JsonProperty("contentDocument")
  @Optional
  public val contentDocument: Node? = null,
  @JsonProperty("shadowRoots")
  @Optional
  public val shadowRoots: List<Node>? = null,
  @JsonProperty("templateContent")
  @Optional
  public val templateContent: Node? = null,
  @JsonProperty("pseudoElements")
  @Optional
  public val pseudoElements: List<Node>? = null,
  @JsonProperty("importedDocument")
  @Optional
  @Deprecated
  public val importedDocument: Node? = null,
  @JsonProperty("distributedNodes")
  @Optional
  public val distributedNodes: List<BackendNode>? = null,
  @JsonProperty("isSVG")
  @Optional
  public val isSVG: Boolean? = null,
)
