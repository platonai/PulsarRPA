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

/** Cookie parameter object */
public class CookieParam {

  private String name;

  private String value;

  @Optional
  private String url;

  @Optional private String domain;

  @Optional private String path;

  @Optional private Boolean secure;

  @Optional private Boolean httpOnly;

  @Optional private CookieSameSite sameSite;

  @Optional private Double expires;

  @Experimental
  @Optional private CookiePriority priority;

  @Experimental @Optional private Boolean sameParty;

  @Experimental @Optional private CookieSourceScheme sourceScheme;

  @Experimental @Optional private Integer sourcePort;

  @Experimental @Optional private String partitionKey;

  /** Cookie name. */
  public String getName() {
    return name;
  }

  /** Cookie name. */
  public void setName(String name) {
    this.name = name;
  }

  /** Cookie value. */
  public String getValue() {
    return value;
  }

  /** Cookie value. */
  public void setValue(String value) {
    this.value = value;
  }

  /**
   * The request-URI to associate with the setting of the cookie. This value can affect the default
   * domain, path, source port, and source scheme values of the created cookie.
   */
  public String getUrl() {
    return url;
  }

  /**
   * The request-URI to associate with the setting of the cookie. This value can affect the default
   * domain, path, source port, and source scheme values of the created cookie.
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /** Cookie domain. */
  public String getDomain() {
    return domain;
  }

  /** Cookie domain. */
  public void setDomain(String domain) {
    this.domain = domain;
  }

  /** Cookie path. */
  public String getPath() {
    return path;
  }

  /** Cookie path. */
  public void setPath(String path) {
    this.path = path;
  }

  /** True if cookie is secure. */
  public Boolean getSecure() {
    return secure;
  }

  /** True if cookie is secure. */
  public void setSecure(Boolean secure) {
    this.secure = secure;
  }

  /** True if cookie is http-only. */
  public Boolean getHttpOnly() {
    return httpOnly;
  }

  /** True if cookie is http-only. */
  public void setHttpOnly(Boolean httpOnly) {
    this.httpOnly = httpOnly;
  }

  /** Cookie SameSite type. */
  public CookieSameSite getSameSite() {
    return sameSite;
  }

  /** Cookie SameSite type. */
  public void setSameSite(CookieSameSite sameSite) {
    this.sameSite = sameSite;
  }

  /** Cookie expiration date, session cookie if not set */
  public Double getExpires() {
    return expires;
  }

  /** Cookie expiration date, session cookie if not set */
  public void setExpires(Double expires) {
    this.expires = expires;
  }

  /** Cookie Priority. */
  public CookiePriority getPriority() {
    return priority;
  }

  /** Cookie Priority. */
  public void setPriority(CookiePriority priority) {
    this.priority = priority;
  }

  /** True if cookie is SameParty. */
  public Boolean getSameParty() {
    return sameParty;
  }

  /** True if cookie is SameParty. */
  public void setSameParty(Boolean sameParty) {
    this.sameParty = sameParty;
  }

  /** Cookie source scheme type. */
  public CookieSourceScheme getSourceScheme() {
    return sourceScheme;
  }

  /** Cookie source scheme type. */
  public void setSourceScheme(CookieSourceScheme sourceScheme) {
    this.sourceScheme = sourceScheme;
  }

  /**
   * Cookie source port. Valid values are {-1, [1, 65535]}, -1 indicates an unspecified port. An
   * unspecified port value allows protocol clients to emulate legacy cookie scope for the port.
   * This is a temporary ability and it will be removed in the future.
   */
  public Integer getSourcePort() {
    return sourcePort;
  }

  /**
   * Cookie source port. Valid values are {-1, [1, 65535]}, -1 indicates an unspecified port. An
   * unspecified port value allows protocol clients to emulate legacy cookie scope for the port.
   * This is a temporary ability and it will be removed in the future.
   */
  public void setSourcePort(Integer sourcePort) {
    this.sourcePort = sourcePort;
  }

  /**
   * Cookie partition key. The site of the top-level URL the browser was visiting at the start of
   * the request to the endpoint that set the cookie. If not set, the cookie will be set as not
   * partitioned.
   */
  public String getPartitionKey() {
    return partitionKey;
  }

  /**
   * Cookie partition key. The site of the top-level URL the browser was visiting at the start of
   * the request to the endpoint that set the cookie. If not set, the cookie will be set as not
   * partitioned.
   */
  public void setPartitionKey(String partitionKey) {
    this.partitionKey = partitionKey;
  }
}
