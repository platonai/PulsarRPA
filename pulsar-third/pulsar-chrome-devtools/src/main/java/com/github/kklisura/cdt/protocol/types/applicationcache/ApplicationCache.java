package com.github.kklisura.cdt.protocol.types.applicationcache;

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

/** Detailed application cache information. */
public class ApplicationCache {

  private String manifestURL;

  private Double size;

  private Double creationTime;

  private Double updateTime;

  private List<ApplicationCacheResource> resources;

  /** Manifest URL. */
  public String getManifestURL() {
    return manifestURL;
  }

  /** Manifest URL. */
  public void setManifestURL(String manifestURL) {
    this.manifestURL = manifestURL;
  }

  /** Application cache size. */
  public Double getSize() {
    return size;
  }

  /** Application cache size. */
  public void setSize(Double size) {
    this.size = size;
  }

  /** Application cache creation time. */
  public Double getCreationTime() {
    return creationTime;
  }

  /** Application cache creation time. */
  public void setCreationTime(Double creationTime) {
    this.creationTime = creationTime;
  }

  /** Application cache update time. */
  public Double getUpdateTime() {
    return updateTime;
  }

  /** Application cache update time. */
  public void setUpdateTime(Double updateTime) {
    this.updateTime = updateTime;
  }

  /** Application cache resources. */
  public List<ApplicationCacheResource> getResources() {
    return resources;
  }

  /** Application cache resources. */
  public void setResources(List<ApplicationCacheResource> resources) {
    this.resources = resources;
  }
}
