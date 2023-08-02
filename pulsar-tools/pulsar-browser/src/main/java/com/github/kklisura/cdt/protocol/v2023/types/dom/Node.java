package com.github.kklisura.cdt.protocol.v2023.types.dom;

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

import java.util.List;

/**
 * DOM interaction is implemented in terms of mirror objects that represent the actual DOM nodes.
 * DOMNode is a base node mirror type.
 */
public class Node {

  private Integer nodeId;

  @Optional
  private Integer parentId;

  private Integer backendNodeId;

  private Integer nodeType;

  private String nodeName;

  private String localName;

  private String nodeValue;

  @Optional private Integer childNodeCount;

  @Optional private List<Node> children;

  @Optional private List<String> attributes;

  @Optional private String documentURL;

  @Optional private String baseURL;

  @Optional private String publicId;

  @Optional private String systemId;

  @Optional private String internalSubset;

  @Optional private String xmlVersion;

  @Optional private String name;

  @Optional private String value;

  @Optional private PseudoType pseudoType;

  @Optional private String pseudoIdentifier;

  @Optional private ShadowRootType shadowRootType;

  @Optional private String frameId;

  @Optional private Node contentDocument;

  @Optional private List<Node> shadowRoots;

  @Optional private Node templateContent;

  @Optional private List<Node> pseudoElements;

  @Deprecated @Optional private Node importedDocument;

  @Optional private List<BackendNode> distributedNodes;

  @Optional private Boolean isSVG;

  @Optional private CompatibilityMode compatibilityMode;

  @Optional private BackendNode assignedSlot;

  /**
   * Node identifier that is passed into the rest of the DOM messages as the `nodeId`. Backend will
   * only push node with given `id` once. It is aware of all requested nodes and will only fire DOM
   * events for nodes known to the client.
   */
  public Integer getNodeId() {
    return nodeId;
  }

  /**
   * Node identifier that is passed into the rest of the DOM messages as the `nodeId`. Backend will
   * only push node with given `id` once. It is aware of all requested nodes and will only fire DOM
   * events for nodes known to the client.
   */
  public void setNodeId(Integer nodeId) {
    this.nodeId = nodeId;
  }

  /** The id of the parent node if any. */
  public Integer getParentId() {
    return parentId;
  }

  /** The id of the parent node if any. */
  public void setParentId(Integer parentId) {
    this.parentId = parentId;
  }

  /** The BackendNodeId for this node. */
  public Integer getBackendNodeId() {
    return backendNodeId;
  }

  /** The BackendNodeId for this node. */
  public void setBackendNodeId(Integer backendNodeId) {
    this.backendNodeId = backendNodeId;
  }

  /** `Node`'s nodeType. */
  public Integer getNodeType() {
    return nodeType;
  }

  /** `Node`'s nodeType. */
  public void setNodeType(Integer nodeType) {
    this.nodeType = nodeType;
  }

  /** `Node`'s nodeName. */
  public String getNodeName() {
    return nodeName;
  }

  /** `Node`'s nodeName. */
  public void setNodeName(String nodeName) {
    this.nodeName = nodeName;
  }

  /** `Node`'s localName. */
  public String getLocalName() {
    return localName;
  }

  /** `Node`'s localName. */
  public void setLocalName(String localName) {
    this.localName = localName;
  }

  /** `Node`'s nodeValue. */
  public String getNodeValue() {
    return nodeValue;
  }

  /** `Node`'s nodeValue. */
  public void setNodeValue(String nodeValue) {
    this.nodeValue = nodeValue;
  }

  /** Child count for `Container` nodes. */
  public Integer getChildNodeCount() {
    return childNodeCount;
  }

  /** Child count for `Container` nodes. */
  public void setChildNodeCount(Integer childNodeCount) {
    this.childNodeCount = childNodeCount;
  }

  /** Child nodes of this node when requested with children. */
  public List<Node> getChildren() {
    return children;
  }

  /** Child nodes of this node when requested with children. */
  public void setChildren(List<Node> children) {
    this.children = children;
  }

  /**
   * Attributes of the `Element` node in the form of flat array `[name1, value1, name2, value2]`.
   */
  public List<String> getAttributes() {
    return attributes;
  }

  /**
   * Attributes of the `Element` node in the form of flat array `[name1, value1, name2, value2]`.
   */
  public void setAttributes(List<String> attributes) {
    this.attributes = attributes;
  }

  /** Document URL that `Document` or `FrameOwner` node points to. */
  public String getDocumentURL() {
    return documentURL;
  }

  /** Document URL that `Document` or `FrameOwner` node points to. */
  public void setDocumentURL(String documentURL) {
    this.documentURL = documentURL;
  }

  /** Base URL that `Document` or `FrameOwner` node uses for URL completion. */
  public String getBaseURL() {
    return baseURL;
  }

  /** Base URL that `Document` or `FrameOwner` node uses for URL completion. */
  public void setBaseURL(String baseURL) {
    this.baseURL = baseURL;
  }

  /** `DocumentType`'s publicId. */
  public String getPublicId() {
    return publicId;
  }

  /** `DocumentType`'s publicId. */
  public void setPublicId(String publicId) {
    this.publicId = publicId;
  }

  /** `DocumentType`'s systemId. */
  public String getSystemId() {
    return systemId;
  }

  /** `DocumentType`'s systemId. */
  public void setSystemId(String systemId) {
    this.systemId = systemId;
  }

  /** `DocumentType`'s internalSubset. */
  public String getInternalSubset() {
    return internalSubset;
  }

  /** `DocumentType`'s internalSubset. */
  public void setInternalSubset(String internalSubset) {
    this.internalSubset = internalSubset;
  }

  /** `Document`'s XML version in case of XML documents. */
  public String getXmlVersion() {
    return xmlVersion;
  }

  /** `Document`'s XML version in case of XML documents. */
  public void setXmlVersion(String xmlVersion) {
    this.xmlVersion = xmlVersion;
  }

  /** `Attr`'s name. */
  public String getName() {
    return name;
  }

  /** `Attr`'s name. */
  public void setName(String name) {
    this.name = name;
  }

  /** `Attr`'s value. */
  public String getValue() {
    return value;
  }

  /** `Attr`'s value. */
  public void setValue(String value) {
    this.value = value;
  }

  /** Pseudo element type for this node. */
  public PseudoType getPseudoType() {
    return pseudoType;
  }

  /** Pseudo element type for this node. */
  public void setPseudoType(PseudoType pseudoType) {
    this.pseudoType = pseudoType;
  }

  /** Pseudo element identifier for this node. Only present if there is a valid pseudoType. */
  public String getPseudoIdentifier() {
    return pseudoIdentifier;
  }

  /** Pseudo element identifier for this node. Only present if there is a valid pseudoType. */
  public void setPseudoIdentifier(String pseudoIdentifier) {
    this.pseudoIdentifier = pseudoIdentifier;
  }

  /** Shadow root type. */
  public ShadowRootType getShadowRootType() {
    return shadowRootType;
  }

  /** Shadow root type. */
  public void setShadowRootType(ShadowRootType shadowRootType) {
    this.shadowRootType = shadowRootType;
  }

  /** Frame ID for frame owner elements. */
  public String getFrameId() {
    return frameId;
  }

  /** Frame ID for frame owner elements. */
  public void setFrameId(String frameId) {
    this.frameId = frameId;
  }

  /** Content document for frame owner elements. */
  public Node getContentDocument() {
    return contentDocument;
  }

  /** Content document for frame owner elements. */
  public void setContentDocument(Node contentDocument) {
    this.contentDocument = contentDocument;
  }

  /** Shadow root list for given element host. */
  public List<Node> getShadowRoots() {
    return shadowRoots;
  }

  /** Shadow root list for given element host. */
  public void setShadowRoots(List<Node> shadowRoots) {
    this.shadowRoots = shadowRoots;
  }

  /** Content document fragment for template elements. */
  public Node getTemplateContent() {
    return templateContent;
  }

  /** Content document fragment for template elements. */
  public void setTemplateContent(Node templateContent) {
    this.templateContent = templateContent;
  }

  /** Pseudo elements associated with this node. */
  public List<Node> getPseudoElements() {
    return pseudoElements;
  }

  /** Pseudo elements associated with this node. */
  public void setPseudoElements(List<Node> pseudoElements) {
    this.pseudoElements = pseudoElements;
  }

  /**
   * Deprecated, as the HTML Imports API has been removed (crbug.com/937746). This property used to
   * return the imported document for the HTMLImport links. The property is always undefined now.
   */
  public Node getImportedDocument() {
    return importedDocument;
  }

  /**
   * Deprecated, as the HTML Imports API has been removed (crbug.com/937746). This property used to
   * return the imported document for the HTMLImport links. The property is always undefined now.
   */
  public void setImportedDocument(Node importedDocument) {
    this.importedDocument = importedDocument;
  }

  /** Distributed nodes for given insertion point. */
  public List<BackendNode> getDistributedNodes() {
    return distributedNodes;
  }

  /** Distributed nodes for given insertion point. */
  public void setDistributedNodes(List<BackendNode> distributedNodes) {
    this.distributedNodes = distributedNodes;
  }

  /** Whether the node is SVG. */
  public Boolean getIsSVG() {
    return isSVG;
  }

  /** Whether the node is SVG. */
  public void setIsSVG(Boolean isSVG) {
    this.isSVG = isSVG;
  }

  public CompatibilityMode getCompatibilityMode() {
    return compatibilityMode;
  }

  public void setCompatibilityMode(CompatibilityMode compatibilityMode) {
    this.compatibilityMode = compatibilityMode;
  }

  public BackendNode getAssignedSlot() {
    return assignedSlot;
  }

  public void setAssignedSlot(BackendNode assignedSlot) {
    this.assignedSlot = assignedSlot;
  }
}
