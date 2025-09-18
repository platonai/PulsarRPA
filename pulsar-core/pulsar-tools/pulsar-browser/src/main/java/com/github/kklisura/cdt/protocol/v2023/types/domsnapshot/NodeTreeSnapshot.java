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

import java.util.List;

/** Table containing nodes. */
public class NodeTreeSnapshot {

  @Optional
  private List<Integer> parentIndex;

  @Optional private List<Integer> nodeType;

  @Optional private RareStringData shadowRootType;

  @Optional private List<Integer> nodeName;

  @Optional private List<Integer> nodeValue;

  @Optional private List<Integer> backendNodeId;

  @Optional private List<List<Integer>> attributes;

  @Optional private RareStringData textValue;

  @Optional private RareStringData inputValue;

  @Optional private RareBooleanData inputChecked;

  @Optional private RareBooleanData optionSelected;

  @Optional private RareIntegerData contentDocumentIndex;

  @Optional private RareStringData pseudoType;

  @Optional private RareStringData pseudoIdentifier;

  @Optional private RareBooleanData isClickable;

  @Optional private RareStringData currentSourceURL;

  @Optional private RareStringData originURL;

  /** Parent node index. */
  public List<Integer> getParentIndex() {
    return parentIndex;
  }

  /** Parent node index. */
  public void setParentIndex(List<Integer> parentIndex) {
    this.parentIndex = parentIndex;
  }

  /** `Node`'s nodeType. */
  public List<Integer> getNodeType() {
    return nodeType;
  }

  /** `Node`'s nodeType. */
  public void setNodeType(List<Integer> nodeType) {
    this.nodeType = nodeType;
  }

  /**
   * Type of the shadow root the `Node` is in. String values are equal to the `ShadowRootType` enum.
   */
  public RareStringData getShadowRootType() {
    return shadowRootType;
  }

  /**
   * Type of the shadow root the `Node` is in. String values are equal to the `ShadowRootType` enum.
   */
  public void setShadowRootType(RareStringData shadowRootType) {
    this.shadowRootType = shadowRootType;
  }

  /** `Node`'s nodeName. */
  public List<Integer> getNodeName() {
    return nodeName;
  }

  /** `Node`'s nodeName. */
  public void setNodeName(List<Integer> nodeName) {
    this.nodeName = nodeName;
  }

  /** `Node`'s nodeValue. */
  public List<Integer> getNodeValue() {
    return nodeValue;
  }

  /** `Node`'s nodeValue. */
  public void setNodeValue(List<Integer> nodeValue) {
    this.nodeValue = nodeValue;
  }

  /** `Node`'s id, corresponds to DOM.Node.backendNodeId. */
  public List<Integer> getBackendNodeId() {
    return backendNodeId;
  }

  /** `Node`'s id, corresponds to DOM.Node.backendNodeId. */
  public void setBackendNodeId(List<Integer> backendNodeId) {
    this.backendNodeId = backendNodeId;
  }

  /** Attributes of an `Element` node. Flatten name, value pairs. */
  public List<List<Integer>> getAttributes() {
    return attributes;
  }

  /** Attributes of an `Element` node. Flatten name, value pairs. */
  public void setAttributes(List<List<Integer>> attributes) {
    this.attributes = attributes;
  }

  /** Only set for textarea elements, contains the text value. */
  public RareStringData getTextValue() {
    return textValue;
  }

  /** Only set for textarea elements, contains the text value. */
  public void setTextValue(RareStringData textValue) {
    this.textValue = textValue;
  }

  /** Only set for input elements, contains the input's associated text value. */
  public RareStringData getInputValue() {
    return inputValue;
  }

  /** Only set for input elements, contains the input's associated text value. */
  public void setInputValue(RareStringData inputValue) {
    this.inputValue = inputValue;
  }

  /** Only set for radio and checkbox input elements, indicates if the element has been checked */
  public RareBooleanData getInputChecked() {
    return inputChecked;
  }

  /** Only set for radio and checkbox input elements, indicates if the element has been checked */
  public void setInputChecked(RareBooleanData inputChecked) {
    this.inputChecked = inputChecked;
  }

  /** Only set for option elements, indicates if the element has been selected */
  public RareBooleanData getOptionSelected() {
    return optionSelected;
  }

  /** Only set for option elements, indicates if the element has been selected */
  public void setOptionSelected(RareBooleanData optionSelected) {
    this.optionSelected = optionSelected;
  }

  /** The index of the document in the list of the snapshot documents. */
  public RareIntegerData getContentDocumentIndex() {
    return contentDocumentIndex;
  }

  /** The index of the document in the list of the snapshot documents. */
  public void setContentDocumentIndex(RareIntegerData contentDocumentIndex) {
    this.contentDocumentIndex = contentDocumentIndex;
  }

  /** Type of a pseudo element node. */
  public RareStringData getPseudoType() {
    return pseudoType;
  }

  /** Type of a pseudo element node. */
  public void setPseudoType(RareStringData pseudoType) {
    this.pseudoType = pseudoType;
  }

  /** Pseudo element identifier for this node. Only present if there is a valid pseudoType. */
  public RareStringData getPseudoIdentifier() {
    return pseudoIdentifier;
  }

  /** Pseudo element identifier for this node. Only present if there is a valid pseudoType. */
  public void setPseudoIdentifier(RareStringData pseudoIdentifier) {
    this.pseudoIdentifier = pseudoIdentifier;
  }

  /**
   * Whether this DOM node responds to mouse clicks. This includes nodes that have had click event
   * listeners attached via JavaScript as well as anchor tags that naturally navigate when clicked.
   */
  public RareBooleanData getIsClickable() {
    return isClickable;
  }

  /**
   * Whether this DOM node responds to mouse clicks. This includes nodes that have had click event
   * listeners attached via JavaScript as well as anchor tags that naturally navigate when clicked.
   */
  public void setIsClickable(RareBooleanData isClickable) {
    this.isClickable = isClickable;
  }

  /** The selected url for nodes with a srcset attribute. */
  public RareStringData getCurrentSourceURL() {
    return currentSourceURL;
  }

  /** The selected url for nodes with a srcset attribute. */
  public void setCurrentSourceURL(RareStringData currentSourceURL) {
    this.currentSourceURL = currentSourceURL;
  }

  /** The url of the script (if any) that generates this node. */
  public RareStringData getOriginURL() {
    return originURL;
  }

  /** The url of the script (if any) that generates this node. */
  public void setOriginURL(RareStringData originURL) {
    this.originURL = originURL;
  }
}
