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

/** Detailed application cache resource information. */
public class ApplicationCacheResource {

  private String url;

  private Integer size;

  private String type;

  /** Resource url. */
  public String getUrl() {
    return url;
  }

  /** Resource url. */
  public void setUrl(String url) {
    this.url = url;
  }

  /** Resource size. */
  public Integer getSize() {
    return size;
  }

  /** Resource size. */
  public void setSize(Integer size) {
    this.size = size;
  }

  /** Resource type. */
  public String getType() {
    return type;
  }

  /** Resource type. */
  public void setType(String type) {
    this.type = type;
  }
}
