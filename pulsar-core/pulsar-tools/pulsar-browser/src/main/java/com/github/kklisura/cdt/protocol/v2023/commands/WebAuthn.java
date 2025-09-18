package com.github.kklisura.cdt.protocol.v2023.commands;

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

import com.github.kklisura.cdt.protocol.v2023.events.webauthn.CredentialAdded;
import com.github.kklisura.cdt.protocol.v2023.events.webauthn.CredentialAsserted;
import com.github.kklisura.cdt.protocol.v2023.support.annotations.*;
import com.github.kklisura.cdt.protocol.v2023.support.types.EventHandler;
import com.github.kklisura.cdt.protocol.v2023.support.types.EventListener;
import com.github.kklisura.cdt.protocol.v2023.types.webauthn.Credential;
import com.github.kklisura.cdt.protocol.v2023.types.webauthn.VirtualAuthenticatorOptions;

import java.util.List;

/** This domain allows configuring virtual authenticators to test the WebAuthn API. */
@Experimental
public interface WebAuthn {

  /**
   * Enable the WebAuthn domain and start intercepting credential storage and retrieval with a
   * virtual authenticator.
   */
  void enable();

  /**
   * Enable the WebAuthn domain and start intercepting credential storage and retrieval with a
   * virtual authenticator.
   *
   * @param enableUI Whether to enable the WebAuthn user interface. Enabling the UI is recommended
   *     for debugging and demo purposes, as it is closer to the real experience. Disabling the UI
   *     is recommended for automated testing. Supported at the embedder's discretion if UI is
   *     available. Defaults to false.
   */
  void enable(@Optional @ParamName("enableUI") Boolean enableUI);

  /** Disable the WebAuthn domain. */
  void disable();

  /**
   * Creates and adds a virtual authenticator.
   *
   * @param options
   */
  @Returns("authenticatorId")
  String addVirtualAuthenticator(@ParamName("options") VirtualAuthenticatorOptions options);

  /**
   * Resets parameters isBogusSignature, isBadUV, isBadUP to false if they are not present.
   *
   * @param authenticatorId
   */
  void setResponseOverrideBits(@ParamName("authenticatorId") String authenticatorId);

  /**
   * Resets parameters isBogusSignature, isBadUV, isBadUP to false if they are not present.
   *
   * @param authenticatorId
   * @param isBogusSignature If isBogusSignature is set, overrides the signature in the
   *     authenticator response to be zero. Defaults to false.
   * @param isBadUV If isBadUV is set, overrides the UV bit in the flags in the authenticator
   *     response to be zero. Defaults to false.
   * @param isBadUP If isBadUP is set, overrides the UP bit in the flags in the authenticator
   *     response to be zero. Defaults to false.
   */
  void setResponseOverrideBits(
      @ParamName("authenticatorId") String authenticatorId,
      @Optional @ParamName("isBogusSignature") Boolean isBogusSignature,
      @Optional @ParamName("isBadUV") Boolean isBadUV,
      @Optional @ParamName("isBadUP") Boolean isBadUP);

  /**
   * Removes the given authenticator.
   *
   * @param authenticatorId
   */
  void removeVirtualAuthenticator(@ParamName("authenticatorId") String authenticatorId);

  /**
   * Adds the credential to the specified authenticator.
   *
   * @param authenticatorId
   * @param credential
   */
  void addCredential(
      @ParamName("authenticatorId") String authenticatorId,
      @ParamName("credential") Credential credential);

  /**
   * Returns a single credential stored in the given virtual authenticator that matches the
   * credential ID.
   *
   * @param authenticatorId
   * @param credentialId
   */
  @Returns("credential")
  Credential getCredential(
      @ParamName("authenticatorId") String authenticatorId,
      @ParamName("credentialId") String credentialId);

  /**
   * Returns all the credentials stored in the given virtual authenticator.
   *
   * @param authenticatorId
   */
  @Returns("credentials")
  @ReturnTypeParameter(Credential.class)
  List<Credential> getCredentials(@ParamName("authenticatorId") String authenticatorId);

  /**
   * Removes a credential from the authenticator.
   *
   * @param authenticatorId
   * @param credentialId
   */
  void removeCredential(
      @ParamName("authenticatorId") String authenticatorId,
      @ParamName("credentialId") String credentialId);

  /**
   * Clears all the credentials from the specified device.
   *
   * @param authenticatorId
   */
  void clearCredentials(@ParamName("authenticatorId") String authenticatorId);

  /**
   * Sets whether User Verification succeeds or fails for an authenticator. The default is true.
   *
   * @param authenticatorId
   * @param isUserVerified
   */
  void setUserVerified(
      @ParamName("authenticatorId") String authenticatorId,
      @ParamName("isUserVerified") Boolean isUserVerified);

  /**
   * Sets whether tests of user presence will succeed immediately (if true) or fail to resolve (if
   * false) for an authenticator. The default is true.
   *
   * @param authenticatorId
   * @param enabled
   */
  void setAutomaticPresenceSimulation(
      @ParamName("authenticatorId") String authenticatorId, @ParamName("enabled") Boolean enabled);

  /** Triggered when a credential is added to an authenticator. */
  @EventName("credentialAdded")
  EventListener onCredentialAdded(EventHandler<CredentialAdded> eventListener);

  /** Triggered when a credential is used in a webauthn assertion. */
  @EventName("credentialAsserted")
  EventListener onCredentialAsserted(EventHandler<CredentialAsserted> eventListener);
}
