package com.github.kklisura.cdt.protocol.v2023.events.network;

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

import java.util.List;

/**
 * Fired once when parsing the .wbn file has succeeded. The event contains the information about the
 * web bundle contents.
 */
@Experimental
public class SubresourceWebBundleMetadataReceived {

  private String requestId;

  private List<String> urls;

  /** Request identifier. Used to match this information to another event. */
  public String getRequestId() {
    return requestId;
  }

  /** Request identifier. Used to match this information to another event. */
  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  /** A list of URLs of resources in the subresource Web Bundle. */
  public List<String> getUrls() {
    return urls;
  }

  /** A list of URLs of resources in the subresource Web Bundle. */
  public void setUrls(List<String> urls) {
    this.urls = urls;
  }
}
