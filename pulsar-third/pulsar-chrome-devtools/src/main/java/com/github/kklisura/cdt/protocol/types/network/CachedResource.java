package com.github.kklisura.cdt.protocol.types.network;

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

/** Information about the cached resource. */
public class CachedResource {

  private String url;

  private ResourceType type;

  @Optional private Response response;

  private Double bodySize;

  /** Resource URL. This is the url of the original network request. */
  public String getUrl() {
    return url;
  }

  /** Resource URL. This is the url of the original network request. */
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

  /** Cached response data. */
  public Response getResponse() {
    return response;
  }

  /** Cached response data. */
  public void setResponse(Response response) {
    this.response = response;
  }

  /** Cached response body size. */
  public Double getBodySize() {
    return bodySize;
  }

  /** Cached response body size. */
  public void setBodySize(Double bodySize) {
    this.bodySize = bodySize;
  }
}
