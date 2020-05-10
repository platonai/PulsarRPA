package com.github.kklisura.cdt.protocol.types.fetch;

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

@Experimental
public class RequestPattern {

  @Optional private String urlPattern;

  @Optional private ResourceType resourceType;

  @Optional private RequestStage requestStage;

  /**
   * Wildcards ('*' -> zero or more, '?' -> exactly one) are allowed. Escape character is backslash.
   * Omitting is equivalent to "*".
   */
  public String getUrlPattern() {
    return urlPattern;
  }

  /**
   * Wildcards ('*' -> zero or more, '?' -> exactly one) are allowed. Escape character is backslash.
   * Omitting is equivalent to "*".
   */
  public void setUrlPattern(String urlPattern) {
    this.urlPattern = urlPattern;
  }

  /** If set, only requests for matching resource types will be intercepted. */
  public ResourceType getResourceType() {
    return resourceType;
  }

  /** If set, only requests for matching resource types will be intercepted. */
  public void setResourceType(ResourceType resourceType) {
    this.resourceType = resourceType;
  }

  /** Stage at wich to begin intercepting requests. Default is Request. */
  public RequestStage getRequestStage() {
    return requestStage;
  }

  /** Stage at wich to begin intercepting requests. Default is Request. */
  public void setRequestStage(RequestStage requestStage) {
    this.requestStage = requestStage;
  }
}
