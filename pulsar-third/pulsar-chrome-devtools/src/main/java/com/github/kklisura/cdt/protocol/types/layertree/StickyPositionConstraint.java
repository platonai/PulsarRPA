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
import com.github.kklisura.cdt.protocol.types.dom.Rect;

/** Sticky position constraints. */
public class StickyPositionConstraint {

  private Rect stickyBoxRect;

  private Rect containingBlockRect;

  @Optional private String nearestLayerShiftingStickyBox;

  @Optional private String nearestLayerShiftingContainingBlock;

  /** Layout rectangle of the sticky element before being shifted */
  public Rect getStickyBoxRect() {
    return stickyBoxRect;
  }

  /** Layout rectangle of the sticky element before being shifted */
  public void setStickyBoxRect(Rect stickyBoxRect) {
    this.stickyBoxRect = stickyBoxRect;
  }

  /** Layout rectangle of the containing block of the sticky element */
  public Rect getContainingBlockRect() {
    return containingBlockRect;
  }

  /** Layout rectangle of the containing block of the sticky element */
  public void setContainingBlockRect(Rect containingBlockRect) {
    this.containingBlockRect = containingBlockRect;
  }

  /** The nearest sticky layer that shifts the sticky box */
  public String getNearestLayerShiftingStickyBox() {
    return nearestLayerShiftingStickyBox;
  }

  /** The nearest sticky layer that shifts the sticky box */
  public void setNearestLayerShiftingStickyBox(String nearestLayerShiftingStickyBox) {
    this.nearestLayerShiftingStickyBox = nearestLayerShiftingStickyBox;
  }

  /** The nearest sticky layer that shifts the containing block */
  public String getNearestLayerShiftingContainingBlock() {
    return nearestLayerShiftingContainingBlock;
  }

  /** The nearest sticky layer that shifts the containing block */
  public void setNearestLayerShiftingContainingBlock(String nearestLayerShiftingContainingBlock) {
    this.nearestLayerShiftingContainingBlock = nearestLayerShiftingContainingBlock;
  }
}
