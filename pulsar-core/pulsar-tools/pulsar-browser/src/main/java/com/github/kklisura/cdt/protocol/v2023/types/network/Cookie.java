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

/** Cookie object */
public class Cookie {

  private String name;

  private String value;

  private String domain;

  private String path;

  private Double expires;

  private Integer size;

  private Boolean httpOnly;

  private Boolean secure;

  private Boolean session;

  @Optional
  private CookieSameSite sameSite;

  @Experimental
  private CookiePriority priority;

  @Experimental private Boolean sameParty;

  @Experimental private CookieSourceScheme sourceScheme;

  @Experimental private Integer sourcePort;

  // vincent: not supported in 2024/11/23
  // Cannot deserialize value of type `java.lang.String` from Object value (token `JsonToken.START_OBJECT`)
  // (through reference chain: java.util.ArrayList[13]->com.github.kklisura.cdt.protocol.v2023.types.network.Cookie["partitionKey"])
//  @Experimental @Optional private String partitionKey;
//
//  @Experimental @Optional private Boolean partitionKeyOpaque;

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

  /** Cookie expiration date as the number of seconds since the UNIX epoch. */
  public Double getExpires() {
    return expires;
  }

  /** Cookie expiration date as the number of seconds since the UNIX epoch. */
  public void setExpires(Double expires) {
    this.expires = expires;
  }

  /** Cookie size. */
  public Integer getSize() {
    return size;
  }

  /** Cookie size. */
  public void setSize(Integer size) {
    this.size = size;
  }

  /** True if cookie is http-only. */
  public Boolean getHttpOnly() {
    return httpOnly;
  }

  /** True if cookie is http-only. */
  public void setHttpOnly(Boolean httpOnly) {
    this.httpOnly = httpOnly;
  }

  /** True if cookie is secure. */
  public Boolean getSecure() {
    return secure;
  }

  /** True if cookie is secure. */
  public void setSecure(Boolean secure) {
    this.secure = secure;
  }

  /** True in case of session cookie. */
  public Boolean getSession() {
    return session;
  }

  /** True in case of session cookie. */
  public void setSession(Boolean session) {
    this.session = session;
  }

  /** Cookie SameSite type. */
  public CookieSameSite getSameSite() {
    return sameSite;
  }

  /** Cookie SameSite type. */
  public void setSameSite(CookieSameSite sameSite) {
    this.sameSite = sameSite;
  }

  /** Cookie Priority */
  public CookiePriority getPriority() {
    return priority;
  }

  /** Cookie Priority */
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

//  /**
//   * Cookie partition key. The site of the top-level URL the browser was visiting at the start of
//   * the request to the endpoint that set the cookie.
//   */
//  public String getPartitionKey() {
//    return partitionKey;
//  }
//
//  /**
//   * Cookie partition key. The site of the top-level URL the browser was visiting at the start of
//   * the request to the endpoint that set the cookie.
//   */
//  public void setPartitionKey(String partitionKey) {
//    this.partitionKey = partitionKey;
//  }
//
//  /** True if cookie partition key is opaque. */
//  public Boolean getPartitionKeyOpaque() {
//    return partitionKeyOpaque;
//  }
//
//  /** True if cookie partition key is opaque. */
//  public void setPartitionKeyOpaque(Boolean partitionKeyOpaque) {
//    this.partitionKeyOpaque = partitionKeyOpaque;
//  }
}
