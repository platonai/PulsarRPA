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
import java.util.List;

/** Table of details of an element in the DOM tree with a LayoutObject. */
public class LayoutTreeSnapshot {

  private List<Integer> nodeIndex;

  private List<List<Integer>> styles;

  private List<List<Double>> bounds;

  private List<Integer> text;

  private RareBooleanData stackingContexts;

  @Optional private List<Integer> paintOrders;

  @Optional private List<List<Double>> offsetRects;

  @Optional private List<List<Double>> scrollRects;

  @Optional private List<List<Double>> clientRects;

  /**
   * Index of the corresponding node in the `NodeTreeSnapshot` array returned by `captureSnapshot`.
   */
  public List<Integer> getNodeIndex() {
    return nodeIndex;
  }

  /**
   * Index of the corresponding node in the `NodeTreeSnapshot` array returned by `captureSnapshot`.
   */
  public void setNodeIndex(List<Integer> nodeIndex) {
    this.nodeIndex = nodeIndex;
  }

  /**
   * Array of indexes specifying computed style strings, filtered according to the `computedStyles`
   * parameter passed to `captureSnapshot`.
   */
  public List<List<Integer>> getStyles() {
    return styles;
  }

  /**
   * Array of indexes specifying computed style strings, filtered according to the `computedStyles`
   * parameter passed to `captureSnapshot`.
   */
  public void setStyles(List<List<Integer>> styles) {
    this.styles = styles;
  }

  /** The absolute position bounding box. */
  public List<List<Double>> getBounds() {
    return bounds;
  }

  /** The absolute position bounding box. */
  public void setBounds(List<List<Double>> bounds) {
    this.bounds = bounds;
  }

  /** Contents of the LayoutText, if any. */
  public List<Integer> getText() {
    return text;
  }

  /** Contents of the LayoutText, if any. */
  public void setText(List<Integer> text) {
    this.text = text;
  }

  /** Stacking context information. */
  public RareBooleanData getStackingContexts() {
    return stackingContexts;
  }

  /** Stacking context information. */
  public void setStackingContexts(RareBooleanData stackingContexts) {
    this.stackingContexts = stackingContexts;
  }

  /**
   * Global paint order index, which is determined by the stacking order of the nodes. Nodes that
   * are painted together will have the same index. Only provided if includePaintOrder in
   * captureSnapshot was true.
   */
  public List<Integer> getPaintOrders() {
    return paintOrders;
  }

  /**
   * Global paint order index, which is determined by the stacking order of the nodes. Nodes that
   * are painted together will have the same index. Only provided if includePaintOrder in
   * captureSnapshot was true.
   */
  public void setPaintOrders(List<Integer> paintOrders) {
    this.paintOrders = paintOrders;
  }

  /** The offset rect of nodes. Only available when includeDOMRects is set to true */
  public List<List<Double>> getOffsetRects() {
    return offsetRects;
  }

  /** The offset rect of nodes. Only available when includeDOMRects is set to true */
  public void setOffsetRects(List<List<Double>> offsetRects) {
    this.offsetRects = offsetRects;
  }

  /** The scroll rect of nodes. Only available when includeDOMRects is set to true */
  public List<List<Double>> getScrollRects() {
    return scrollRects;
  }

  /** The scroll rect of nodes. Only available when includeDOMRects is set to true */
  public void setScrollRects(List<List<Double>> scrollRects) {
    this.scrollRects = scrollRects;
  }

  /** The client rect of nodes. Only available when includeDOMRects is set to true */
  public List<List<Double>> getClientRects() {
    return clientRects;
  }

  /** The client rect of nodes. Only available when includeDOMRects is set to true */
  public void setClientRects(List<List<Double>> clientRects) {
    this.clientRects = clientRects;
  }
}
