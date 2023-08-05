package com.github.kklisura.cdt.protocol.v2023.events.fetch;

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
import com.github.kklisura.cdt.protocol.v2023.types.fetch.HeaderEntry;
import com.github.kklisura.cdt.protocol.v2023.types.network.ErrorReason;
import com.github.kklisura.cdt.protocol.v2023.types.network.Request;
import com.github.kklisura.cdt.protocol.v2023.types.network.ResourceType;

import java.util.List;

/**
 * Issued when the domain is enabled and the request URL matches the specified filter. The request
 * is paused until the client responds with one of continueRequest, failRequest or fulfillRequest.
 * The stage of the request can be determined by presence of responseErrorReason and
 * responseStatusCode -- the request is at the response stage if either of these fields is present
 * and in the request stage otherwise. Redirect responses and subsequent requests are reported
 * similarly to regular responses and requests. Redirect responses may be distinguished by the value
 * of `responseStatusCode` (which is one of 301, 302, 303, 307, 308) along with presence of the
 * `location` header. Requests resulting from a redirect will have `redirectedRequestId` field set.
 */
public class RequestPaused {

  private String requestId;

  private Request request;

  private String frameId;

  private ResourceType resourceType;

  @Optional
  private ErrorReason responseErrorReason;

  @Optional private Integer responseStatusCode;

  @Optional private String responseStatusText;

  @Optional private List<HeaderEntry> responseHeaders;

  @Optional private String networkId;

  @Experimental
  @Optional private String redirectedRequestId;

  /** Each request the page makes will have a unique id. */
  public String getRequestId() {
    return requestId;
  }

  /** Each request the page makes will have a unique id. */
  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  /** The details of the request. */
  public Request getRequest() {
    return request;
  }

  /** The details of the request. */
  public void setRequest(Request request) {
    this.request = request;
  }

  /** The id of the frame that initiated the request. */
  public String getFrameId() {
    return frameId;
  }

  /** The id of the frame that initiated the request. */
  public void setFrameId(String frameId) {
    this.frameId = frameId;
  }

  /** How the requested resource will be used. */
  public ResourceType getResourceType() {
    return resourceType;
  }

  /** How the requested resource will be used. */
  public void setResourceType(ResourceType resourceType) {
    this.resourceType = resourceType;
  }

  /** Response error if intercepted at response stage. */
  public ErrorReason getResponseErrorReason() {
    return responseErrorReason;
  }

  /** Response error if intercepted at response stage. */
  public void setResponseErrorReason(ErrorReason responseErrorReason) {
    this.responseErrorReason = responseErrorReason;
  }

  /** Response code if intercepted at response stage. */
  public Integer getResponseStatusCode() {
    return responseStatusCode;
  }

  /** Response code if intercepted at response stage. */
  public void setResponseStatusCode(Integer responseStatusCode) {
    this.responseStatusCode = responseStatusCode;
  }

  /** Response status text if intercepted at response stage. */
  public String getResponseStatusText() {
    return responseStatusText;
  }

  /** Response status text if intercepted at response stage. */
  public void setResponseStatusText(String responseStatusText) {
    this.responseStatusText = responseStatusText;
  }

  /** Response headers if intercepted at the response stage. */
  public List<HeaderEntry> getResponseHeaders() {
    return responseHeaders;
  }

  /** Response headers if intercepted at the response stage. */
  public void setResponseHeaders(List<HeaderEntry> responseHeaders) {
    this.responseHeaders = responseHeaders;
  }

  /**
   * If the intercepted request had a corresponding Network.requestWillBeSent event fired for it,
   * then this networkId will be the same as the requestId present in the requestWillBeSent event.
   */
  public String getNetworkId() {
    return networkId;
  }

  /**
   * If the intercepted request had a corresponding Network.requestWillBeSent event fired for it,
   * then this networkId will be the same as the requestId present in the requestWillBeSent event.
   */
  public void setNetworkId(String networkId) {
    this.networkId = networkId;
  }

  /**
   * If the request is due to a redirect response from the server, the id of the request that has
   * caused the redirect.
   */
  public String getRedirectedRequestId() {
    return redirectedRequestId;
  }

  /**
   * If the request is due to a redirect response from the server, the id of the request that has
   * caused the redirect.
   */
  public void setRedirectedRequestId(String redirectedRequestId) {
    this.redirectedRequestId = redirectedRequestId;
  }
}
