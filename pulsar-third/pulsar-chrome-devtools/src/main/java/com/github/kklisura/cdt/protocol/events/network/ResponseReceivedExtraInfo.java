package com.github.kklisura.cdt.protocol.events.network;

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
import com.github.kklisura.cdt.protocol.types.network.BlockedSetCookieWithReason;
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

  @Optional private String headersText;

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
}
