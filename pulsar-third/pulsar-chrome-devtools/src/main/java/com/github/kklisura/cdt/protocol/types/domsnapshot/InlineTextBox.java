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

import com.github.kklisura.cdt.protocol.types.dom.Rect;

/**
 * Details of post layout rendered text positions. The exact layout should not be regarded as stable
 * and may change between versions.
 */
public class InlineTextBox {

  private Rect boundingBox;

  private Integer startCharacterIndex;

  private Integer numCharacters;

  /**
   * The bounding box in document coordinates. Note that scroll offset of the document is ignored.
   */
  public Rect getBoundingBox() {
    return boundingBox;
  }

  /**
   * The bounding box in document coordinates. Note that scroll offset of the document is ignored.
   */
  public void setBoundingBox(Rect boundingBox) {
    this.boundingBox = boundingBox;
  }

  /**
   * The starting index in characters, for this post layout textbox substring. Characters that would
   * be represented as a surrogate pair in UTF-16 have length 2.
   */
  public Integer getStartCharacterIndex() {
    return startCharacterIndex;
  }

  /**
   * The starting index in characters, for this post layout textbox substring. Characters that would
   * be represented as a surrogate pair in UTF-16 have length 2.
   */
  public void setStartCharacterIndex(Integer startCharacterIndex) {
    this.startCharacterIndex = startCharacterIndex;
  }

  /**
   * The number of characters in this post layout textbox substring. Characters that would be
   * represented as a surrogate pair in UTF-16 have length 2.
   */
  public Integer getNumCharacters() {
    return numCharacters;
  }

  /**
   * The number of characters in this post layout textbox substring. Characters that would be
   * represented as a surrogate pair in UTF-16 have length 2.
   */
  public void setNumCharacters(Integer numCharacters) {
    this.numCharacters = numCharacters;
  }
}
