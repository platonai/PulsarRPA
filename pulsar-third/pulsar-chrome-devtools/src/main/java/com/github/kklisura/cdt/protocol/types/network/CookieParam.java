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

/** Cookie parameter object */
public class CookieParam {

  private String name;

  private String value;

  @Optional private String url;

  @Optional private String domain;

  @Optional private String path;

  @Optional private Boolean secure;

  @Optional private Boolean httpOnly;

  @Optional private CookieSameSite sameSite;

  @Optional private Double expires;

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
   * domain and path values of the created cookie.
   */
  public String getUrl() {
    return url;
  }

  /**
   * The request-URI to associate with the setting of the cookie. This value can affect the default
   * domain and path values of the created cookie.
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
}
