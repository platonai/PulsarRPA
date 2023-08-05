package com.github.kklisura.cdt.protocol.v2023.types.webauthn;

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

public class VirtualAuthenticatorOptions {

  private AuthenticatorProtocol protocol;

  @Optional
  private Ctap2Version ctap2Version;

  private AuthenticatorTransport transport;

  @Optional private Boolean hasResidentKey;

  @Optional private Boolean hasUserVerification;

  @Optional private Boolean hasLargeBlob;

  @Optional private Boolean hasCredBlob;

  @Optional private Boolean hasMinPinLength;

  @Optional private Boolean hasPrf;

  @Optional private Boolean automaticPresenceSimulation;

  @Optional private Boolean isUserVerified;

  public AuthenticatorProtocol getProtocol() {
    return protocol;
  }

  public void setProtocol(AuthenticatorProtocol protocol) {
    this.protocol = protocol;
  }

  /** Defaults to ctap2_0. Ignored if |protocol| == u2f. */
  public Ctap2Version getCtap2Version() {
    return ctap2Version;
  }

  /** Defaults to ctap2_0. Ignored if |protocol| == u2f. */
  public void setCtap2Version(Ctap2Version ctap2Version) {
    this.ctap2Version = ctap2Version;
  }

  public AuthenticatorTransport getTransport() {
    return transport;
  }

  public void setTransport(AuthenticatorTransport transport) {
    this.transport = transport;
  }

  /** Defaults to false. */
  public Boolean getHasResidentKey() {
    return hasResidentKey;
  }

  /** Defaults to false. */
  public void setHasResidentKey(Boolean hasResidentKey) {
    this.hasResidentKey = hasResidentKey;
  }

  /** Defaults to false. */
  public Boolean getHasUserVerification() {
    return hasUserVerification;
  }

  /** Defaults to false. */
  public void setHasUserVerification(Boolean hasUserVerification) {
    this.hasUserVerification = hasUserVerification;
  }

  /**
   * If set to true, the authenticator will support the largeBlob extension.
   * https://w3c.github.io/webauthn#largeBlob Defaults to false.
   */
  public Boolean getHasLargeBlob() {
    return hasLargeBlob;
  }

  /**
   * If set to true, the authenticator will support the largeBlob extension.
   * https://w3c.github.io/webauthn#largeBlob Defaults to false.
   */
  public void setHasLargeBlob(Boolean hasLargeBlob) {
    this.hasLargeBlob = hasLargeBlob;
  }

  /**
   * If set to true, the authenticator will support the credBlob extension.
   * https://fidoalliance.org/specs/fido-v2.1-rd-20201208/fido-client-to-authenticator-protocol-v2.1-rd-20201208.html#sctn-credBlob-extension
   * Defaults to false.
   */
  public Boolean getHasCredBlob() {
    return hasCredBlob;
  }

  /**
   * If set to true, the authenticator will support the credBlob extension.
   * https://fidoalliance.org/specs/fido-v2.1-rd-20201208/fido-client-to-authenticator-protocol-v2.1-rd-20201208.html#sctn-credBlob-extension
   * Defaults to false.
   */
  public void setHasCredBlob(Boolean hasCredBlob) {
    this.hasCredBlob = hasCredBlob;
  }

  /**
   * If set to true, the authenticator will support the minPinLength extension.
   * https://fidoalliance.org/specs/fido-v2.1-ps-20210615/fido-client-to-authenticator-protocol-v2.1-ps-20210615.html#sctn-minpinlength-extension
   * Defaults to false.
   */
  public Boolean getHasMinPinLength() {
    return hasMinPinLength;
  }

  /**
   * If set to true, the authenticator will support the minPinLength extension.
   * https://fidoalliance.org/specs/fido-v2.1-ps-20210615/fido-client-to-authenticator-protocol-v2.1-ps-20210615.html#sctn-minpinlength-extension
   * Defaults to false.
   */
  public void setHasMinPinLength(Boolean hasMinPinLength) {
    this.hasMinPinLength = hasMinPinLength;
  }

  /**
   * If set to true, the authenticator will support the prf extension.
   * https://w3c.github.io/webauthn/#prf-extension Defaults to false.
   */
  public Boolean getHasPrf() {
    return hasPrf;
  }

  /**
   * If set to true, the authenticator will support the prf extension.
   * https://w3c.github.io/webauthn/#prf-extension Defaults to false.
   */
  public void setHasPrf(Boolean hasPrf) {
    this.hasPrf = hasPrf;
  }

  /**
   * If set to true, tests of user presence will succeed immediately. Otherwise, they will not be
   * resolved. Defaults to true.
   */
  public Boolean getAutomaticPresenceSimulation() {
    return automaticPresenceSimulation;
  }

  /**
   * If set to true, tests of user presence will succeed immediately. Otherwise, they will not be
   * resolved. Defaults to true.
   */
  public void setAutomaticPresenceSimulation(Boolean automaticPresenceSimulation) {
    this.automaticPresenceSimulation = automaticPresenceSimulation;
  }

  /** Sets whether User Verification succeeds or fails for an authenticator. Defaults to false. */
  public Boolean getIsUserVerified() {
    return isUserVerified;
  }

  /** Sets whether User Verification succeeds or fails for an authenticator. Defaults to false. */
  public void setIsUserVerified(Boolean isUserVerified) {
    this.isUserVerified = isUserVerified;
  }
}
