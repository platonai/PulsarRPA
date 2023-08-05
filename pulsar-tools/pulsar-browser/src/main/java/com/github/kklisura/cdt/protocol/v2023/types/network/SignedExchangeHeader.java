package com.github.kklisura.cdt.protocol.v2023.types.network;

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
import java.util.Map;

/**
 * Information about a signed exchange header.
 * https://wicg.github.io/webpackage/draft-yasskin-httpbis-origin-signed-exchanges-impl.html#cbor-representation
 */
@Experimental
public class SignedExchangeHeader {

  private String requestUrl;

  private Integer responseCode;

  private Map<String, Object> responseHeaders;

  private List<SignedExchangeSignature> signatures;

  private String headerIntegrity;

  /** Signed exchange request URL. */
  public String getRequestUrl() {
    return requestUrl;
  }

  /** Signed exchange request URL. */
  public void setRequestUrl(String requestUrl) {
    this.requestUrl = requestUrl;
  }

  /** Signed exchange response code. */
  public Integer getResponseCode() {
    return responseCode;
  }

  /** Signed exchange response code. */
  public void setResponseCode(Integer responseCode) {
    this.responseCode = responseCode;
  }

  /** Signed exchange response headers. */
  public Map<String, Object> getResponseHeaders() {
    return responseHeaders;
  }

  /** Signed exchange response headers. */
  public void setResponseHeaders(Map<String, Object> responseHeaders) {
    this.responseHeaders = responseHeaders;
  }

  /** Signed exchange response signature. */
  public List<SignedExchangeSignature> getSignatures() {
    return signatures;
  }

  /** Signed exchange response signature. */
  public void setSignatures(List<SignedExchangeSignature> signatures) {
    this.signatures = signatures;
  }

  /** Signed exchange header integrity hash in the form of `sha256-<base64-hash-value>`. */
  public String getHeaderIntegrity() {
    return headerIntegrity;
  }

  /** Signed exchange header integrity hash in the form of `sha256-<base64-hash-value>`. */
  public void setHeaderIntegrity(String headerIntegrity) {
    this.headerIntegrity = headerIntegrity;
  }
}
