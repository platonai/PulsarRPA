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

import com.github.kklisura.cdt.protocol.support.annotations.Experimental;
import com.github.kklisura.cdt.protocol.support.annotations.Optional;
import java.util.List;

/**
 * Information about a signed exchange signature.
 * https://wicg.github.io/webpackage/draft-yasskin-httpbis-origin-signed-exchanges-impl.html#rfc.section.3.1
 */
@Experimental
public class SignedExchangeSignature {

  private String label;

  private String signature;

  private String integrity;

  @Optional private String certUrl;

  @Optional private String certSha256;

  private String validityUrl;

  private Integer date;

  private Integer expires;

  @Optional private List<String> certificates;

  /** Signed exchange signature label. */
  public String getLabel() {
    return label;
  }

  /** Signed exchange signature label. */
  public void setLabel(String label) {
    this.label = label;
  }

  /** The hex string of signed exchange signature. */
  public String getSignature() {
    return signature;
  }

  /** The hex string of signed exchange signature. */
  public void setSignature(String signature) {
    this.signature = signature;
  }

  /** Signed exchange signature integrity. */
  public String getIntegrity() {
    return integrity;
  }

  /** Signed exchange signature integrity. */
  public void setIntegrity(String integrity) {
    this.integrity = integrity;
  }

  /** Signed exchange signature cert Url. */
  public String getCertUrl() {
    return certUrl;
  }

  /** Signed exchange signature cert Url. */
  public void setCertUrl(String certUrl) {
    this.certUrl = certUrl;
  }

  /** The hex string of signed exchange signature cert sha256. */
  public String getCertSha256() {
    return certSha256;
  }

  /** The hex string of signed exchange signature cert sha256. */
  public void setCertSha256(String certSha256) {
    this.certSha256 = certSha256;
  }

  /** Signed exchange signature validity Url. */
  public String getValidityUrl() {
    return validityUrl;
  }

  /** Signed exchange signature validity Url. */
  public void setValidityUrl(String validityUrl) {
    this.validityUrl = validityUrl;
  }

  /** Signed exchange signature date. */
  public Integer getDate() {
    return date;
  }

  /** Signed exchange signature date. */
  public void setDate(Integer date) {
    this.date = date;
  }

  /** Signed exchange signature expires. */
  public Integer getExpires() {
    return expires;
  }

  /** Signed exchange signature expires. */
  public void setExpires(Integer expires) {
    this.expires = expires;
  }

  /** The encoded certificates. */
  public List<String> getCertificates() {
    return certificates;
  }

  /** The encoded certificates. */
  public void setCertificates(List<String> certificates) {
    this.certificates = certificates;
  }
}
