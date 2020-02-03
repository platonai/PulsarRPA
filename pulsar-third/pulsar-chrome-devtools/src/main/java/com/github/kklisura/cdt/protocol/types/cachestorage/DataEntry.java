package com.github.kklisura.cdt.protocol.types.cachestorage;

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

/** Data entry. */
public class DataEntry {

  private String requestURL;

  private String requestMethod;

  private List<Header> requestHeaders;

  private Double responseTime;

  private Integer responseStatus;

  private String responseStatusText;

  private CachedResponseType responseType;

  private List<Header> responseHeaders;

  /** Request URL. */
  public String getRequestURL() {
    return requestURL;
  }

  /** Request URL. */
  public void setRequestURL(String requestURL) {
    this.requestURL = requestURL;
  }

  /** Request method. */
  public String getRequestMethod() {
    return requestMethod;
  }

  /** Request method. */
  public void setRequestMethod(String requestMethod) {
    this.requestMethod = requestMethod;
  }

  /** Request headers */
  public List<Header> getRequestHeaders() {
    return requestHeaders;
  }

  /** Request headers */
  public void setRequestHeaders(List<Header> requestHeaders) {
    this.requestHeaders = requestHeaders;
  }

  /** Number of seconds since epoch. */
  public Double getResponseTime() {
    return responseTime;
  }

  /** Number of seconds since epoch. */
  public void setResponseTime(Double responseTime) {
    this.responseTime = responseTime;
  }

  /** HTTP response status code. */
  public Integer getResponseStatus() {
    return responseStatus;
  }

  /** HTTP response status code. */
  public void setResponseStatus(Integer responseStatus) {
    this.responseStatus = responseStatus;
  }

  /** HTTP response status text. */
  public String getResponseStatusText() {
    return responseStatusText;
  }

  /** HTTP response status text. */
  public void setResponseStatusText(String responseStatusText) {
    this.responseStatusText = responseStatusText;
  }

  /** HTTP response type */
  public CachedResponseType getResponseType() {
    return responseType;
  }

  /** HTTP response type */
  public void setResponseType(CachedResponseType responseType) {
    this.responseType = responseType;
  }

  /** Response headers */
  public List<Header> getResponseHeaders() {
    return responseHeaders;
  }

  /** Response headers */
  public void setResponseHeaders(List<Header> responseHeaders) {
    this.responseHeaders = responseHeaders;
  }
}
