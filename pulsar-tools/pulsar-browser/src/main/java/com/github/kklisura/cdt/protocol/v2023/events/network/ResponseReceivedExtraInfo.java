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
import com.github.kklisura.cdt.protocol.v2023.support.annotations.Optional;
import com.github.kklisura.cdt.protocol.v2023.types.network.BlockedSetCookieWithReason;
import com.github.kklisura.cdt.protocol.v2023.types.network.IPAddressSpace;

import java.util.List;
import java.util.Map;

/**
 * Fired when additional information about a responseReceived event is available from the network
 * stack. Not every responseReceived event will have an additional responseReceivedExtraInfo for it,
 * and responseReceivedExtraInfo may be fired before or after responseReceived.
 */
@Experimental
public class ResponseReceivedExtraInfo {

  private String requestId;

  private List<BlockedSetCookieWithReason> blockedCookies;

  private Map<String, Object> headers;

  private IPAddressSpace resourceIPAddressSpace;

  private Integer statusCode;

  @Optional
  private String headersText;

  @Optional private String cookiePartitionKey;

  @Optional private Boolean cookiePartitionKeyOpaque;

  /** Request identifier. Used to match this information to another responseReceived event. */
  public String getRequestId() {
    return requestId;
  }

  /** Request identifier. Used to match this information to another responseReceived event. */
  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  /**
   * A list of cookies which were not stored from the response along with the corresponding reasons
   * for blocking. The cookies here may not be valid due to syntax errors, which are represented by
   * the invalid cookie line string instead of a proper cookie.
   */
  public List<BlockedSetCookieWithReason> getBlockedCookies() {
    return blockedCookies;
  }

  /**
   * A list of cookies which were not stored from the response along with the corresponding reasons
   * for blocking. The cookies here may not be valid due to syntax errors, which are represented by
   * the invalid cookie line string instead of a proper cookie.
   */
  public void setBlockedCookies(List<BlockedSetCookieWithReason> blockedCookies) {
    this.blockedCookies = blockedCookies;
  }

  /** Raw response headers as they were received over the wire. */
  public Map<String, Object> getHeaders() {
    return headers;
  }

  /** Raw response headers as they were received over the wire. */
  public void setHeaders(Map<String, Object> headers) {
    this.headers = headers;
  }

  /**
   * The IP address space of the resource. The address space can only be determined once the
   * transport established the connection, so we can't send it in `requestWillBeSentExtraInfo`.
   */
  public IPAddressSpace getResourceIPAddressSpace() {
    return resourceIPAddressSpace;
  }

  /**
   * The IP address space of the resource. The address space can only be determined once the
   * transport established the connection, so we can't send it in `requestWillBeSentExtraInfo`.
   */
  public void setResourceIPAddressSpace(IPAddressSpace resourceIPAddressSpace) {
    this.resourceIPAddressSpace = resourceIPAddressSpace;
  }

  /**
   * The status code of the response. This is useful in cases the request failed and no
   * responseReceived event is triggered, which is the case for, e.g., CORS errors. This is also the
   * correct status code for cached requests, where the status in responseReceived is a 200 and this
   * will be 304.
   */
  public Integer getStatusCode() {
    return statusCode;
  }

  /**
   * The status code of the response. This is useful in cases the request failed and no
   * responseReceived event is triggered, which is the case for, e.g., CORS errors. This is also the
   * correct status code for cached requests, where the status in responseReceived is a 200 and this
   * will be 304.
   */
  public void setStatusCode(Integer statusCode) {
    this.statusCode = statusCode;
  }

  /**
   * Raw response header text as it was received over the wire. The raw text may not always be
   * available, such as in the case of HTTP/2 or QUIC.
   */
  public String getHeadersText() {
    return headersText;
  }

  /**
   * Raw response header text as it was received over the wire. The raw text may not always be
   * available, such as in the case of HTTP/2 or QUIC.
   */
  public void setHeadersText(String headersText) {
    this.headersText = headersText;
  }

  /**
   * The cookie partition key that will be used to store partitioned cookies set in this response.
   * Only sent when partitioned cookies are enabled.
   */
  public String getCookiePartitionKey() {
    return cookiePartitionKey;
  }

  /**
   * The cookie partition key that will be used to store partitioned cookies set in this response.
   * Only sent when partitioned cookies are enabled.
   */
  public void setCookiePartitionKey(String cookiePartitionKey) {
    this.cookiePartitionKey = cookiePartitionKey;
  }

  /**
   * True if partitioned cookies are enabled, but the partition key is not serializeable to string.
   */
  public Boolean getCookiePartitionKeyOpaque() {
    return cookiePartitionKeyOpaque;
  }

  /**
   * True if partitioned cookies are enabled, but the partition key is not serializeable to string.
   */
  public void setCookiePartitionKeyOpaque(Boolean cookiePartitionKeyOpaque) {
    this.cookiePartitionKeyOpaque = cookiePartitionKeyOpaque;
  }
}
