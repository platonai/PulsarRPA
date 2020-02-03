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
import java.util.Map;

/** WebSocket response data. */
public class WebSocketResponse {

  private Integer status;

  private String statusText;

  private Map<String, Object> headers;

  @Optional private String headersText;

  @Optional private Map<String, Object> requestHeaders;

  @Optional private String requestHeadersText;

  /** HTTP response status code. */
  public Integer getStatus() {
    return status;
  }

  /** HTTP response status code. */
  public void setStatus(Integer status) {
    this.status = status;
  }

  /** HTTP response status text. */
  public String getStatusText() {
    return statusText;
  }

  /** HTTP response status text. */
  public void setStatusText(String statusText) {
    this.statusText = statusText;
  }

  /** HTTP response headers. */
  public Map<String, Object> getHeaders() {
    return headers;
  }

  /** HTTP response headers. */
  public void setHeaders(Map<String, Object> headers) {
    this.headers = headers;
  }

  /** HTTP response headers text. */
  public String getHeadersText() {
    return headersText;
  }

  /** HTTP response headers text. */
  public void setHeadersText(String headersText) {
    this.headersText = headersText;
  }

  /** HTTP request headers. */
  public Map<String, Object> getRequestHeaders() {
    return requestHeaders;
  }

  /** HTTP request headers. */
  public void setRequestHeaders(Map<String, Object> requestHeaders) {
    this.requestHeaders = requestHeaders;
  }

  /** HTTP request headers text. */
  public String getRequestHeadersText() {
    return requestHeadersText;
  }

  /** HTTP request headers text. */
  public void setRequestHeadersText(String requestHeadersText) {
    this.requestHeadersText = requestHeadersText;
  }
}
