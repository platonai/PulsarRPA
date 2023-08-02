package com.github.kklisura.cdt.protocol.v2023.types.css;

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

import com.github.kklisura.cdt.protocol.v2023.support.annotations.Experimental;
import com.github.kklisura.cdt.protocol.v2023.support.annotations.Optional;

import java.util.List;

/** CSS Layer data. */
@Experimental
public class CSSLayerData {

  private String name;

  @Optional
  private List<CSSLayerData> subLayers;

  private Double order;

  /** Layer name. */
  public String getName() {
    return name;
  }

  /** Layer name. */
  public void setName(String name) {
    this.name = name;
  }

  /** Direct sub-layers */
  public List<CSSLayerData> getSubLayers() {
    return subLayers;
  }

  /** Direct sub-layers */
  public void setSubLayers(List<CSSLayerData> subLayers) {
    this.subLayers = subLayers;
  }

  /**
   * Layer order. The order determines the order of the layer in the cascade order. A higher number
   * has higher priority in the cascade order.
   */
  public Double getOrder() {
    return order;
  }

  /**
   * Layer order. The order determines the order of the layer in the cascade order. A higher number
   * has higher priority in the cascade order.
   */
  public void setOrder(Double order) {
    this.order = order;
  }
}
