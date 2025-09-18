package com.github.kklisura.cdt.protocol.v2023.types.emulation;

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

public class DisplayFeature {

  private DisplayFeatureOrientation orientation;

  private Integer offset;

  private Integer maskLength;

  /** Orientation of a display feature in relation to screen */
  public DisplayFeatureOrientation getOrientation() {
    return orientation;
  }

  /** Orientation of a display feature in relation to screen */
  public void setOrientation(DisplayFeatureOrientation orientation) {
    this.orientation = orientation;
  }

  /**
   * The offset from the screen origin in either the x (for vertical orientation) or y (for
   * horizontal orientation) direction.
   */
  public Integer getOffset() {
    return offset;
  }

  /**
   * The offset from the screen origin in either the x (for vertical orientation) or y (for
   * horizontal orientation) direction.
   */
  public void setOffset(Integer offset) {
    this.offset = offset;
  }

  /**
   * A display feature may mask content such that it is not physically displayed - this length along
   * with the offset describes this area. A display feature that only splits content will have a 0
   * mask_length.
   */
  public Integer getMaskLength() {
    return maskLength;
  }

  /**
   * A display feature may mask content such that it is not physically displayed - this length along
   * with the offset describes this area. A display feature that only splits content will have a 0
   * mask_length.
   */
  public void setMaskLength(Integer maskLength) {
    this.maskLength = maskLength;
  }
}
