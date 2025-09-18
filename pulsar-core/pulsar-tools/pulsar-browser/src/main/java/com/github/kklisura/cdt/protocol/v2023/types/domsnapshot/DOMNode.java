package com.github.kklisura.cdt.protocol.v2023.types.domsnapshot;

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
import com.github.kklisura.cdt.protocol.v2023.types.dom.PseudoType;
import com.github.kklisura.cdt.protocol.v2023.types.dom.ShadowRootType;
import com.github.kklisura.cdt.protocol.v2023.types.domdebugger.EventListener;

import java.util.List;

/** A Node in the DOM tree. */
public class DOMNode {

  private Integer nodeType;

  private String nodeName;

  private String nodeValue;

  @Optional
  private String textValue;

  @Optional private String inputValue;

  @Optional private Boolean inputChecked;

  @Optional private Boolean optionSelected;

  private Integer backendNodeId;

  @Optional private List<Integer> childNodeIndexes;

  @Optional private List<NameValue> attributes;

  @Optional private List<Integer> pseudoElementIndexes;

  @Optional private Integer layoutNodeIndex;

  @Optional private String documentURL;

  @Optional private String baseURL;

  @Optional private String contentLanguage;

  @Optional private String documentEncoding;

  @Optional private String publicId;

  @Optional private String systemId;

  @Optional private String frameId;

  @Optional private Integer contentDocumentIndex;

  @Optional private PseudoType pseudoType;

  @Optional private ShadowRootType shadowRootType;

  @Optional private Boolean isClickable;

  @Optional private List<EventListener> eventListeners;

  @Optional private String currentSourceURL;

  @Optional private String originURL;

  @Optional private Double scrollOffsetX;

  @Optional private Double scrollOffsetY;

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

  /** `Node`'s nodeValue. */
  public String getNodeValue() {
    return nodeValue;
  }

  /** `Node`'s nodeValue. */
  public void setNodeValue(String nodeValue) {
    this.nodeValue = nodeValue;
  }

  /** Only set for textarea elements, contains the text value. */
  public String getTextValue() {
    return textValue;
  }

  /** Only set for textarea elements, contains the text value. */
  public void setTextValue(String textValue) {
    this.textValue = textValue;
  }

  /** Only set for input elements, contains the input's associated text value. */
  public String getInputValue() {
    return inputValue;
  }

  /** Only set for input elements, contains the input's associated text value. */
  public void setInputValue(String inputValue) {
    this.inputValue = inputValue;
  }

  /** Only set for radio and checkbox input elements, indicates if the element has been checked */
  public Boolean getInputChecked() {
    return inputChecked;
  }

  /** Only set for radio and checkbox input elements, indicates if the element has been checked */
  public void setInputChecked(Boolean inputChecked) {
    this.inputChecked = inputChecked;
  }

  /** Only set for option elements, indicates if the element has been selected */
  public Boolean getOptionSelected() {
    return optionSelected;
  }

  /** Only set for option elements, indicates if the element has been selected */
  public void setOptionSelected(Boolean optionSelected) {
    this.optionSelected = optionSelected;
  }

  /** `Node`'s id, corresponds to DOM.Node.backendNodeId. */
  public Integer getBackendNodeId() {
    return backendNodeId;
  }

  /** `Node`'s id, corresponds to DOM.Node.backendNodeId. */
  public void setBackendNodeId(Integer backendNodeId) {
    this.backendNodeId = backendNodeId;
  }

  /**
   * The indexes of the node's child nodes in the `domNodes` array returned by `getSnapshot`, if
   * any.
   */
  public List<Integer> getChildNodeIndexes() {
    return childNodeIndexes;
  }

  /**
   * The indexes of the node's child nodes in the `domNodes` array returned by `getSnapshot`, if
   * any.
   */
  public void setChildNodeIndexes(List<Integer> childNodeIndexes) {
    this.childNodeIndexes = childNodeIndexes;
  }

  /** Attributes of an `Element` node. */
  public List<NameValue> getAttributes() {
    return attributes;
  }

  /** Attributes of an `Element` node. */
  public void setAttributes(List<NameValue> attributes) {
    this.attributes = attributes;
  }

  /**
   * Indexes of pseudo elements associated with this node in the `domNodes` array returned by
   * `getSnapshot`, if any.
   */
  public List<Integer> getPseudoElementIndexes() {
    return pseudoElementIndexes;
  }

  /**
   * Indexes of pseudo elements associated with this node in the `domNodes` array returned by
   * `getSnapshot`, if any.
   */
  public void setPseudoElementIndexes(List<Integer> pseudoElementIndexes) {
    this.pseudoElementIndexes = pseudoElementIndexes;
  }

  /**
   * The index of the node's related layout tree node in the `layoutTreeNodes` array returned by
   * `getSnapshot`, if any.
   */
  public Integer getLayoutNodeIndex() {
    return layoutNodeIndex;
  }

  /**
   * The index of the node's related layout tree node in the `layoutTreeNodes` array returned by
   * `getSnapshot`, if any.
   */
  public void setLayoutNodeIndex(Integer layoutNodeIndex) {
    this.layoutNodeIndex = layoutNodeIndex;
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

  /** Only set for documents, contains the document's content language. */
  public String getContentLanguage() {
    return contentLanguage;
  }

  /** Only set for documents, contains the document's content language. */
  public void setContentLanguage(String contentLanguage) {
    this.contentLanguage = contentLanguage;
  }

  /** Only set for documents, contains the document's character set encoding. */
  public String getDocumentEncoding() {
    return documentEncoding;
  }

  /** Only set for documents, contains the document's character set encoding. */
  public void setDocumentEncoding(String documentEncoding) {
    this.documentEncoding = documentEncoding;
  }

  /** `DocumentType` node's publicId. */
  public String getPublicId() {
    return publicId;
  }

  /** `DocumentType` node's publicId. */
  public void setPublicId(String publicId) {
    this.publicId = publicId;
  }

  /** `DocumentType` node's systemId. */
  public String getSystemId() {
    return systemId;
  }

  /** `DocumentType` node's systemId. */
  public void setSystemId(String systemId) {
    this.systemId = systemId;
  }

  /** Frame ID for frame owner elements and also for the document node. */
  public String getFrameId() {
    return frameId;
  }

  /** Frame ID for frame owner elements and also for the document node. */
  public void setFrameId(String frameId) {
    this.frameId = frameId;
  }

  /**
   * The index of a frame owner element's content document in the `domNodes` array returned by
   * `getSnapshot`, if any.
   */
  public Integer getContentDocumentIndex() {
    return contentDocumentIndex;
  }

  /**
   * The index of a frame owner element's content document in the `domNodes` array returned by
   * `getSnapshot`, if any.
   */
  public void setContentDocumentIndex(Integer contentDocumentIndex) {
    this.contentDocumentIndex = contentDocumentIndex;
  }

  /** Type of a pseudo element node. */
  public PseudoType getPseudoType() {
    return pseudoType;
  }

  /** Type of a pseudo element node. */
  public void setPseudoType(PseudoType pseudoType) {
    this.pseudoType = pseudoType;
  }

  /** Shadow root type. */
  public ShadowRootType getShadowRootType() {
    return shadowRootType;
  }

  /** Shadow root type. */
  public void setShadowRootType(ShadowRootType shadowRootType) {
    this.shadowRootType = shadowRootType;
  }

  /**
   * Whether this DOM node responds to mouse clicks. This includes nodes that have had click event
   * listeners attached via JavaScript as well as anchor tags that naturally navigate when clicked.
   */
  public Boolean getIsClickable() {
    return isClickable;
  }

  /**
   * Whether this DOM node responds to mouse clicks. This includes nodes that have had click event
   * listeners attached via JavaScript as well as anchor tags that naturally navigate when clicked.
   */
  public void setIsClickable(Boolean isClickable) {
    this.isClickable = isClickable;
  }

  /** Details of the node's event listeners, if any. */
  public List<EventListener> getEventListeners() {
    return eventListeners;
  }

  /** Details of the node's event listeners, if any. */
  public void setEventListeners(List<EventListener> eventListeners) {
    this.eventListeners = eventListeners;
  }

  /** The selected url for nodes with a srcset attribute. */
  public String getCurrentSourceURL() {
    return currentSourceURL;
  }

  /** The selected url for nodes with a srcset attribute. */
  public void setCurrentSourceURL(String currentSourceURL) {
    this.currentSourceURL = currentSourceURL;
  }

  /** The url of the script (if any) that generates this node. */
  public String getOriginURL() {
    return originURL;
  }

  /** The url of the script (if any) that generates this node. */
  public void setOriginURL(String originURL) {
    this.originURL = originURL;
  }

  /** Scroll offsets, set when this node is a Document. */
  public Double getScrollOffsetX() {
    return scrollOffsetX;
  }

  /** Scroll offsets, set when this node is a Document. */
  public void setScrollOffsetX(Double scrollOffsetX) {
    this.scrollOffsetX = scrollOffsetX;
  }

  public Double getScrollOffsetY() {
    return scrollOffsetY;
  }

  public void setScrollOffsetY(Double scrollOffsetY) {
    this.scrollOffsetY = scrollOffsetY;
  }
}
