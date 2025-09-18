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

import com.github.kklisura.cdt.protocol.v2023.support.annotations.Optional;

import java.util.List;

/** Security details about a request. */
public class SecurityDetails {

  private String protocol;

  private String keyExchange;

  @Optional
  private String keyExchangeGroup;

  private String cipher;

  @Optional private String mac;

  private Integer certificateId;

  private String subjectName;

  private List<String> sanList;

  private String issuer;

  private Double validFrom;

  private Double validTo;

  private List<SignedCertificateTimestamp> signedCertificateTimestampList;

  private CertificateTransparencyCompliance certificateTransparencyCompliance;

  @Optional private Integer serverSignatureAlgorithm;

  private Boolean encryptedClientHello;

  /** Protocol name (e.g. "TLS 1.2" or "QUIC"). */
  public String getProtocol() {
    return protocol;
  }

  /** Protocol name (e.g. "TLS 1.2" or "QUIC"). */
  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  /** Key Exchange used by the connection, or the empty string if not applicable. */
  public String getKeyExchange() {
    return keyExchange;
  }

  /** Key Exchange used by the connection, or the empty string if not applicable. */
  public void setKeyExchange(String keyExchange) {
    this.keyExchange = keyExchange;
  }

  /** (EC)DH group used by the connection, if applicable. */
  public String getKeyExchangeGroup() {
    return keyExchangeGroup;
  }

  /** (EC)DH group used by the connection, if applicable. */
  public void setKeyExchangeGroup(String keyExchangeGroup) {
    this.keyExchangeGroup = keyExchangeGroup;
  }

  /** Cipher name. */
  public String getCipher() {
    return cipher;
  }

  /** Cipher name. */
  public void setCipher(String cipher) {
    this.cipher = cipher;
  }

  /** TLS MAC. Note that AEAD ciphers do not have separate MACs. */
  public String getMac() {
    return mac;
  }

  /** TLS MAC. Note that AEAD ciphers do not have separate MACs. */
  public void setMac(String mac) {
    this.mac = mac;
  }

  /** Certificate ID value. */
  public Integer getCertificateId() {
    return certificateId;
  }

  /** Certificate ID value. */
  public void setCertificateId(Integer certificateId) {
    this.certificateId = certificateId;
  }

  /** Certificate subject name. */
  public String getSubjectName() {
    return subjectName;
  }

  /** Certificate subject name. */
  public void setSubjectName(String subjectName) {
    this.subjectName = subjectName;
  }

  /** Subject Alternative Name (SAN) DNS names and IP addresses. */
  public List<String> getSanList() {
    return sanList;
  }

  /** Subject Alternative Name (SAN) DNS names and IP addresses. */
  public void setSanList(List<String> sanList) {
    this.sanList = sanList;
  }

  /** Name of the issuing CA. */
  public String getIssuer() {
    return issuer;
  }

  /** Name of the issuing CA. */
  public void setIssuer(String issuer) {
    this.issuer = issuer;
  }

  /** Certificate valid from date. */
  public Double getValidFrom() {
    return validFrom;
  }

  /** Certificate valid from date. */
  public void setValidFrom(Double validFrom) {
    this.validFrom = validFrom;
  }

  /** Certificate valid to (expiration) date */
  public Double getValidTo() {
    return validTo;
  }

  /** Certificate valid to (expiration) date */
  public void setValidTo(Double validTo) {
    this.validTo = validTo;
  }

  /** List of signed certificate timestamps (SCTs). */
  public List<SignedCertificateTimestamp> getSignedCertificateTimestampList() {
    return signedCertificateTimestampList;
  }

  /** List of signed certificate timestamps (SCTs). */
  public void setSignedCertificateTimestampList(
      List<SignedCertificateTimestamp> signedCertificateTimestampList) {
    this.signedCertificateTimestampList = signedCertificateTimestampList;
  }

  /** Whether the request complied with Certificate Transparency policy */
  public CertificateTransparencyCompliance getCertificateTransparencyCompliance() {
    return certificateTransparencyCompliance;
  }

  /** Whether the request complied with Certificate Transparency policy */
  public void setCertificateTransparencyCompliance(
      CertificateTransparencyCompliance certificateTransparencyCompliance) {
    this.certificateTransparencyCompliance = certificateTransparencyCompliance;
  }

  /**
   * The signature algorithm used by the server in the TLS server signature, represented as a TLS
   * SignatureScheme code point. Omitted if not applicable or not known.
   */
  public Integer getServerSignatureAlgorithm() {
    return serverSignatureAlgorithm;
  }

  /**
   * The signature algorithm used by the server in the TLS server signature, represented as a TLS
   * SignatureScheme code point. Omitted if not applicable or not known.
   */
  public void setServerSignatureAlgorithm(Integer serverSignatureAlgorithm) {
    this.serverSignatureAlgorithm = serverSignatureAlgorithm;
  }

  /** Whether the connection used Encrypted ClientHello */
  public Boolean getEncryptedClientHello() {
    return encryptedClientHello;
  }

  /** Whether the connection used Encrypted ClientHello */
  public void setEncryptedClientHello(Boolean encryptedClientHello) {
    this.encryptedClientHello = encryptedClientHello;
  }
}
