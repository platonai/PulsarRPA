package com.github.kklisura.cdt.protocol.types.layertree;

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
import java.util.List;

/** Information about a compositing layer. */
public class Layer {

  private String layerId;

  @Optional private String parentLayerId;

  @Optional private Integer backendNodeId;

  private Double offsetX;

  private Double offsetY;

  private Double width;

  private Double height;

  @Optional private List<Double> transform;

  @Optional private Double anchorX;

  @Optional private Double anchorY;

  @Optional private Double anchorZ;

  private Integer paintCount;

  private Boolean drawsContent;

  @Optional private Boolean invisible;

  @Optional private List<ScrollRect> scrollRects;

  @Optional private StickyPositionConstraint stickyPositionConstraint;

  /** The unique id for this layer. */
  public String getLayerId() {
    return layerId;
  }

  /** The unique id for this layer. */
  public void setLayerId(String layerId) {
    this.layerId = layerId;
  }

  /** The id of parent (not present for root). */
  public String getParentLayerId() {
    return parentLayerId;
  }

  /** The id of parent (not present for root). */
  public void setParentLayerId(String parentLayerId) {
    this.parentLayerId = parentLayerId;
  }

  /** The backend id for the node associated with this layer. */
  public Integer getBackendNodeId() {
    return backendNodeId;
  }

  /** The backend id for the node associated with this layer. */
  public void setBackendNodeId(Integer backendNodeId) {
    this.backendNodeId = backendNodeId;
  }

  /** Offset from parent layer, X coordinate. */
  public Double getOffsetX() {
    return offsetX;
  }

  /** Offset from parent layer, X coordinate. */
  public void setOffsetX(Double offsetX) {
    this.offsetX = offsetX;
  }

  /** Offset from parent layer, Y coordinate. */
  public Double getOffsetY() {
    return offsetY;
  }

  /** Offset from parent layer, Y coordinate. */
  public void setOffsetY(Double offsetY) {
    this.offsetY = offsetY;
  }

  /** Layer width. */
  public Double getWidth() {
    return width;
  }

  /** Layer width. */
  public void setWidth(Double width) {
    this.width = width;
  }

  /** Layer height. */
  public Double getHeight() {
    return height;
  }

  /** Layer height. */
  public void setHeight(Double height) {
    this.height = height;
  }

  /** Transformation matrix for layer, default is identity matrix */
  public List<Double> getTransform() {
    return transform;
  }

  /** Transformation matrix for layer, default is identity matrix */
  public void setTransform(List<Double> transform) {
    this.transform = transform;
  }

  /** Transform anchor point X, absent if no transform specified */
  public Double getAnchorX() {
    return anchorX;
  }

  /** Transform anchor point X, absent if no transform specified */
  public void setAnchorX(Double anchorX) {
    this.anchorX = anchorX;
  }

  /** Transform anchor point Y, absent if no transform specified */
  public Double getAnchorY() {
    return anchorY;
  }

  /** Transform anchor point Y, absent if no transform specified */
  public void setAnchorY(Double anchorY) {
    this.anchorY = anchorY;
  }

  /** Transform anchor point Z, absent if no transform specified */
  public Double getAnchorZ() {
    return anchorZ;
  }

  /** Transform anchor point Z, absent if no transform specified */
  public void setAnchorZ(Double anchorZ) {
    this.anchorZ = anchorZ;
  }

  /** Indicates how many time this layer has painted. */
  public Integer getPaintCount() {
    return paintCount;
  }

  /** Indicates how many time this layer has painted. */
  public void setPaintCount(Integer paintCount) {
    this.paintCount = paintCount;
  }

  /**
   * Indicates whether this layer hosts any content, rather than being used for transform/scrolling
   * purposes only.
   */
  public Boolean getDrawsContent() {
    return drawsContent;
  }

  /**
   * Indicates whether this layer hosts any content, rather than being used for transform/scrolling
   * purposes only.
   */
  public void setDrawsContent(Boolean drawsContent) {
    this.drawsContent = drawsContent;
  }

  /** Set if layer is not visible. */
  public Boolean getInvisible() {
    return invisible;
  }

  /** Set if layer is not visible. */
  public void setInvisible(Boolean invisible) {
    this.invisible = invisible;
  }

  /** Rectangles scrolling on main thread only. */
  public List<ScrollRect> getScrollRects() {
    return scrollRects;
  }

  /** Rectangles scrolling on main thread only. */
  public void setScrollRects(List<ScrollRect> scrollRects) {
    this.scrollRects = scrollRects;
  }

  /** Sticky position constraint information */
  public StickyPositionConstraint getStickyPositionConstraint() {
    return stickyPositionConstraint;
  }

  /** Sticky position constraint information */
  public void setStickyPositionConstraint(StickyPositionConstraint stickyPositionConstraint) {
    this.stickyPositionConstraint = stickyPositionConstraint;
  }
}
