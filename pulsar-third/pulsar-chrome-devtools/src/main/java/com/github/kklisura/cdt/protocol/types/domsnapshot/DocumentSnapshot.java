package com.github.kklisura.cdt.protocol.types.domsnapshot;

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

import com.github.kklisura.cdt.protocol.support.annotations.Optional;

/** Document snapshot. */
public class DocumentSnapshot {

  private Integer documentURL;

  private Integer baseURL;

  private Integer contentLanguage;

  private Integer encodingName;

  private Integer publicId;

  private Integer systemId;

  private Integer frameId;

  private NodeTreeSnapshot nodes;

  private LayoutTreeSnapshot layout;

  private TextBoxSnapshot textBoxes;

  @Optional private Double scrollOffsetX;

  @Optional private Double scrollOffsetY;

  /** Document URL that `Document` or `FrameOwner` node points to. */
  public Integer getDocumentURL() {
    return documentURL;
  }

  /** Document URL that `Document` or `FrameOwner` node points to. */
  public void setDocumentURL(Integer documentURL) {
    this.documentURL = documentURL;
  }

  /** Base URL that `Document` or `FrameOwner` node uses for URL completion. */
  public Integer getBaseURL() {
    return baseURL;
  }

  /** Base URL that `Document` or `FrameOwner` node uses for URL completion. */
  public void setBaseURL(Integer baseURL) {
    this.baseURL = baseURL;
  }

  /** Contains the document's content language. */
  public Integer getContentLanguage() {
    return contentLanguage;
  }

  /** Contains the document's content language. */
  public void setContentLanguage(Integer contentLanguage) {
    this.contentLanguage = contentLanguage;
  }

  /** Contains the document's character set encoding. */
  public Integer getEncodingName() {
    return encodingName;
  }

  /** Contains the document's character set encoding. */
  public void setEncodingName(Integer encodingName) {
    this.encodingName = encodingName;
  }

  /** `DocumentType` node's publicId. */
  public Integer getPublicId() {
    return publicId;
  }

  /** `DocumentType` node's publicId. */
  public void setPublicId(Integer publicId) {
    this.publicId = publicId;
  }

  /** `DocumentType` node's systemId. */
  public Integer getSystemId() {
    return systemId;
  }

  /** `DocumentType` node's systemId. */
  public void setSystemId(Integer systemId) {
    this.systemId = systemId;
  }

  /** Frame ID for frame owner elements and also for the document node. */
  public Integer getFrameId() {
    return frameId;
  }

  /** Frame ID for frame owner elements and also for the document node. */
  public void setFrameId(Integer frameId) {
    this.frameId = frameId;
  }

  /** A table with dom nodes. */
  public NodeTreeSnapshot getNodes() {
    return nodes;
  }

  /** A table with dom nodes. */
  public void setNodes(NodeTreeSnapshot nodes) {
    this.nodes = nodes;
  }

  /** The nodes in the layout tree. */
  public LayoutTreeSnapshot getLayout() {
    return layout;
  }

  /** The nodes in the layout tree. */
  public void setLayout(LayoutTreeSnapshot layout) {
    this.layout = layout;
  }

  /** The post-layout inline text nodes. */
  public TextBoxSnapshot getTextBoxes() {
    return textBoxes;
  }

  /** The post-layout inline text nodes. */
  public void setTextBoxes(TextBoxSnapshot textBoxes) {
    this.textBoxes = textBoxes;
  }

  /** Horizontal scroll offset. */
  public Double getScrollOffsetX() {
    return scrollOffsetX;
  }

  /** Horizontal scroll offset. */
  public void setScrollOffsetX(Double scrollOffsetX) {
    this.scrollOffsetX = scrollOffsetX;
  }

  /** Vertical scroll offset. */
  public Double getScrollOffsetY() {
    return scrollOffsetY;
  }

  /** Vertical scroll offset. */
  public void setScrollOffsetY(Double scrollOffsetY) {
    this.scrollOffsetY = scrollOffsetY;
  }
}
