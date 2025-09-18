package com.github.kklisura.cdt.protocol.v2023.types.performancetimeline;

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

/** See https://github.com/WICG/LargestContentfulPaint and largest_contentful_paint.idl */
public class LargestContentfulPaint {

  private Double renderTime;

  private Double loadTime;

  private Double size;

  @Optional
  private String elementId;

  @Optional private String url;

  @Optional private Integer nodeId;

  public Double getRenderTime() {
    return renderTime;
  }

  public void setRenderTime(Double renderTime) {
    this.renderTime = renderTime;
  }

  public Double getLoadTime() {
    return loadTime;
  }

  public void setLoadTime(Double loadTime) {
    this.loadTime = loadTime;
  }

  /** The number of pixels being painted. */
  public Double getSize() {
    return size;
  }

  /** The number of pixels being painted. */
  public void setSize(Double size) {
    this.size = size;
  }

  /** The id attribute of the element, if available. */
  public String getElementId() {
    return elementId;
  }

  /** The id attribute of the element, if available. */
  public void setElementId(String elementId) {
    this.elementId = elementId;
  }

  /** The URL of the image (may be trimmed). */
  public String getUrl() {
    return url;
  }

  /** The URL of the image (may be trimmed). */
  public void setUrl(String url) {
    this.url = url;
  }

  public Integer getNodeId() {
    return nodeId;
  }

  public void setNodeId(Integer nodeId) {
    this.nodeId = nodeId;
  }
}
