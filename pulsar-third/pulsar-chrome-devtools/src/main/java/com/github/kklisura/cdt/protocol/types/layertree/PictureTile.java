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

/** Serialized fragment of layer picture along with its offset within the layer. */
public class PictureTile {

  private Double x;

  private Double y;

  private String picture;

  /** Offset from owning layer left boundary */
  public Double getX() {
    return x;
  }

  /** Offset from owning layer left boundary */
  public void setX(Double x) {
    this.x = x;
  }

  /** Offset from owning layer top boundary */
  public Double getY() {
    return y;
  }

  /** Offset from owning layer top boundary */
  public void setY(Double y) {
    this.y = y;
  }

  /** Base64-encoded snapshot data. */
  public String getPicture() {
    return picture;
  }

  /** Base64-encoded snapshot data. */
  public void setPicture(String picture) {
    this.picture = picture;
  }
}
