package com.github.kklisura.cdt.protocol.types.page;

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

import com.github.kklisura.cdt.protocol.support.annotations.Experimental;
import com.github.kklisura.cdt.protocol.support.annotations.Optional;
import com.github.kklisura.cdt.protocol.types.network.ResourceType;

/** Information about the Resource on the page. */
@Experimental
public class FrameResource {

  private String url;

  private ResourceType type;

  private String mimeType;

  @Optional private Double lastModified;

  @Optional private Double contentSize;

  @Optional private Boolean failed;

  @Optional private Boolean canceled;

  /** Resource URL. */
  public String getUrl() {
    return url;
  }

  /** Resource URL. */
  public void setUrl(String url) {
    this.url = url;
  }

  /** Type of this resource. */
  public ResourceType getType() {
    return type;
  }

  /** Type of this resource. */
  public void setType(ResourceType type) {
    this.type = type;
  }

  /** Resource mimeType as determined by the browser. */
  public String getMimeType() {
    return mimeType;
  }

  /** Resource mimeType as determined by the browser. */
  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  /** last-modified timestamp as reported by server. */
  public Double getLastModified() {
    return lastModified;
  }

  /** last-modified timestamp as reported by server. */
  public void setLastModified(Double lastModified) {
    this.lastModified = lastModified;
  }

  /** Resource content size. */
  public Double getContentSize() {
    return contentSize;
  }

  /** Resource content size. */
  public void setContentSize(Double contentSize) {
    this.contentSize = contentSize;
  }

  /** True if the resource failed to load. */
  public Boolean getFailed() {
    return failed;
  }

  /** True if the resource failed to load. */
  public void setFailed(Boolean failed) {
    this.failed = failed;
  }

  /** True if the resource was canceled during loading. */
  public Boolean getCanceled() {
    return canceled;
  }

  /** True if the resource was canceled during loading. */
  public void setCanceled(Boolean canceled) {
    this.canceled = canceled;
  }
}
