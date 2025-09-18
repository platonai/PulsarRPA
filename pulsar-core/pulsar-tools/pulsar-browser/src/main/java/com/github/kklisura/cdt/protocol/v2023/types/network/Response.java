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
import com.github.kklisura.cdt.protocol.v2023.support.annotations.Optional;
import com.github.kklisura.cdt.protocol.v2023.types.security.SecurityState;

import java.util.Map;

/** HTTP response data. */
public class Response {

  private String url;

  private Integer status;

  private String statusText;

  private Map<String, Object> headers;

  @Deprecated @Optional
  private String headersText;

  private String mimeType;

  @Optional private Map<String, Object> requestHeaders;

  @Deprecated @Optional private String requestHeadersText;

  private Boolean connectionReused;

  private Double connectionId;

  @Optional private String remoteIPAddress;

  @Optional private Integer remotePort;

  @Optional private Boolean fromDiskCache;

  @Optional private Boolean fromServiceWorker;

  @Optional private Boolean fromPrefetchCache;

  private Double encodedDataLength;

  @Optional private ResourceTiming timing;

  @Optional private ServiceWorkerResponseSource serviceWorkerResponseSource;

  @Optional private Double responseTime;

  @Optional private String cacheStorageCacheName;

  @Optional private String protocol;

  @Experimental
  @Optional private AlternateProtocolUsage alternateProtocolUsage;

  private SecurityState securityState;

  @Optional private SecurityDetails securityDetails;

  /** Response URL. This URL can be different from CachedResource.url in case of redirect. */
  public String getUrl() {
    return url;
  }

  /** Response URL. This URL can be different from CachedResource.url in case of redirect. */
  public void setUrl(String url) {
    this.url = url;
  }

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

  /**
   * HTTP response headers text. This has been replaced by the headers in
   * Network.responseReceivedExtraInfo.
   */
  public String getHeadersText() {
    return headersText;
  }

  /**
   * HTTP response headers text. This has been replaced by the headers in
   * Network.responseReceivedExtraInfo.
   */
  public void setHeadersText(String headersText) {
    this.headersText = headersText;
  }

  /** Resource mimeType as determined by the browser. */
  public String getMimeType() {
    return mimeType;
  }

  /** Resource mimeType as determined by the browser. */
  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  /** Refined HTTP request headers that were actually transmitted over the network. */
  public Map<String, Object> getRequestHeaders() {
    return requestHeaders;
  }

  /** Refined HTTP request headers that were actually transmitted over the network. */
  public void setRequestHeaders(Map<String, Object> requestHeaders) {
    this.requestHeaders = requestHeaders;
  }

  /**
   * HTTP request headers text. This has been replaced by the headers in
   * Network.requestWillBeSentExtraInfo.
   */
  public String getRequestHeadersText() {
    return requestHeadersText;
  }

  /**
   * HTTP request headers text. This has been replaced by the headers in
   * Network.requestWillBeSentExtraInfo.
   */
  public void setRequestHeadersText(String requestHeadersText) {
    this.requestHeadersText = requestHeadersText;
  }

  /** Specifies whether physical connection was actually reused for this request. */
  public Boolean getConnectionReused() {
    return connectionReused;
  }

  /** Specifies whether physical connection was actually reused for this request. */
  public void setConnectionReused(Boolean connectionReused) {
    this.connectionReused = connectionReused;
  }

  /** Physical connection id that was actually used for this request. */
  public Double getConnectionId() {
    return connectionId;
  }

  /** Physical connection id that was actually used for this request. */
  public void setConnectionId(Double connectionId) {
    this.connectionId = connectionId;
  }

  /** Remote IP address. */
  public String getRemoteIPAddress() {
    return remoteIPAddress;
  }

  /** Remote IP address. */
  public void setRemoteIPAddress(String remoteIPAddress) {
    this.remoteIPAddress = remoteIPAddress;
  }

  /** Remote port. */
  public Integer getRemotePort() {
    return remotePort;
  }

  /** Remote port. */
  public void setRemotePort(Integer remotePort) {
    this.remotePort = remotePort;
  }

  /** Specifies that the request was served from the disk cache. */
  public Boolean getFromDiskCache() {
    return fromDiskCache;
  }

  /** Specifies that the request was served from the disk cache. */
  public void setFromDiskCache(Boolean fromDiskCache) {
    this.fromDiskCache = fromDiskCache;
  }

  /** Specifies that the request was served from the ServiceWorker. */
  public Boolean getFromServiceWorker() {
    return fromServiceWorker;
  }

  /** Specifies that the request was served from the ServiceWorker. */
  public void setFromServiceWorker(Boolean fromServiceWorker) {
    this.fromServiceWorker = fromServiceWorker;
  }

  /** Specifies that the request was served from the prefetch cache. */
  public Boolean getFromPrefetchCache() {
    return fromPrefetchCache;
  }

  /** Specifies that the request was served from the prefetch cache. */
  public void setFromPrefetchCache(Boolean fromPrefetchCache) {
    this.fromPrefetchCache = fromPrefetchCache;
  }

  /** Total number of bytes received for this request so far. */
  public Double getEncodedDataLength() {
    return encodedDataLength;
  }

  /** Total number of bytes received for this request so far. */
  public void setEncodedDataLength(Double encodedDataLength) {
    this.encodedDataLength = encodedDataLength;
  }

  /** Timing information for the given request. */
  public ResourceTiming getTiming() {
    return timing;
  }

  /** Timing information for the given request. */
  public void setTiming(ResourceTiming timing) {
    this.timing = timing;
  }

  /** Response source of response from ServiceWorker. */
  public ServiceWorkerResponseSource getServiceWorkerResponseSource() {
    return serviceWorkerResponseSource;
  }

  /** Response source of response from ServiceWorker. */
  public void setServiceWorkerResponseSource(
      ServiceWorkerResponseSource serviceWorkerResponseSource) {
    this.serviceWorkerResponseSource = serviceWorkerResponseSource;
  }

  /** The time at which the returned response was generated. */
  public Double getResponseTime() {
    return responseTime;
  }

  /** The time at which the returned response was generated. */
  public void setResponseTime(Double responseTime) {
    this.responseTime = responseTime;
  }

  /** Cache Storage Cache Name. */
  public String getCacheStorageCacheName() {
    return cacheStorageCacheName;
  }

  /** Cache Storage Cache Name. */
  public void setCacheStorageCacheName(String cacheStorageCacheName) {
    this.cacheStorageCacheName = cacheStorageCacheName;
  }

  /** Protocol used to fetch this request. */
  public String getProtocol() {
    return protocol;
  }

  /** Protocol used to fetch this request. */
  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  /** The reason why Chrome uses a specific transport protocol for HTTP semantics. */
  public AlternateProtocolUsage getAlternateProtocolUsage() {
    return alternateProtocolUsage;
  }

  /** The reason why Chrome uses a specific transport protocol for HTTP semantics. */
  public void setAlternateProtocolUsage(AlternateProtocolUsage alternateProtocolUsage) {
    this.alternateProtocolUsage = alternateProtocolUsage;
  }

  /** Security state of the request resource. */
  public SecurityState getSecurityState() {
    return securityState;
  }

  /** Security state of the request resource. */
  public void setSecurityState(SecurityState securityState) {
    this.securityState = securityState;
  }

  /** Security details for the request. */
  public SecurityDetails getSecurityDetails() {
    return securityDetails;
  }

  /** Security details for the request. */
  public void setSecurityDetails(SecurityDetails securityDetails) {
    this.securityDetails = securityDetails;
  }
}
