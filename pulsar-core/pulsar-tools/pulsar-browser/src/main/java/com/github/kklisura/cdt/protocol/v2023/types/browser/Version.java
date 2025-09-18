package com.github.kklisura.cdt.protocol.v2023.types.browser;

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

public class Version {

  private String protocolVersion;

  private String product;

  private String revision;

  private String userAgent;

  private String jsVersion;

  /** Protocol version. */
  public String getProtocolVersion() {
    return protocolVersion;
  }

  /** Protocol version. */
  public void setProtocolVersion(String protocolVersion) {
    this.protocolVersion = protocolVersion;
  }

  /** Product name. */
  public String getProduct() {
    return product;
  }

  /** Product name. */
  public void setProduct(String product) {
    this.product = product;
  }

  /** Product revision. */
  public String getRevision() {
    return revision;
  }

  /** Product revision. */
  public void setRevision(String revision) {
    this.revision = revision;
  }

  /** User-Agent. */
  public String getUserAgent() {
    return userAgent;
  }

  /** User-Agent. */
  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }

  /** V8 version. */
  public String getJsVersion() {
    return jsVersion;
  }

  /** V8 version. */
  public void setJsVersion(String jsVersion) {
    this.jsVersion = jsVersion;
  }
}
