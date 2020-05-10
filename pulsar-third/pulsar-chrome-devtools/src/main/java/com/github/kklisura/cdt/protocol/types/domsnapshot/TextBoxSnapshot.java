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

import java.util.List;

/**
 * Table of details of the post layout rendered text positions. The exact layout should not be
 * regarded as stable and may change between versions.
 */
public class TextBoxSnapshot {

  private List<Integer> layoutIndex;

  private List<List<Double>> bounds;

  private List<Integer> start;

  private List<Integer> length;

  /** Index of the layout tree node that owns this box collection. */
  public List<Integer> getLayoutIndex() {
    return layoutIndex;
  }

  /** Index of the layout tree node that owns this box collection. */
  public void setLayoutIndex(List<Integer> layoutIndex) {
    this.layoutIndex = layoutIndex;
  }

  /** The absolute position bounding box. */
  public List<List<Double>> getBounds() {
    return bounds;
  }

  /** The absolute position bounding box. */
  public void setBounds(List<List<Double>> bounds) {
    this.bounds = bounds;
  }

  /**
   * The starting index in characters, for this post layout textbox substring. Characters that would
   * be represented as a surrogate pair in UTF-16 have length 2.
   */
  public List<Integer> getStart() {
    return start;
  }

  /**
   * The starting index in characters, for this post layout textbox substring. Characters that would
   * be represented as a surrogate pair in UTF-16 have length 2.
   */
  public void setStart(List<Integer> start) {
    this.start = start;
  }

  /**
   * The number of characters in this post layout textbox substring. Characters that would be
   * represented as a surrogate pair in UTF-16 have length 2.
   */
  public List<Integer> getLength() {
    return length;
  }

  /**
   * The number of characters in this post layout textbox substring. Characters that would be
   * represented as a surrogate pair in UTF-16 have length 2.
   */
  public void setLength(List<Integer> length) {
    this.length = length;
  }
}
