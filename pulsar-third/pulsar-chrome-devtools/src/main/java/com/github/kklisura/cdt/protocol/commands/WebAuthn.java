package com.github.kklisura.cdt.protocol.commands;

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
import com.github.kklisura.cdt.protocol.support.annotations.ParamName;
import com.github.kklisura.cdt.protocol.support.annotations.ReturnTypeParameter;
import com.github.kklisura.cdt.protocol.support.annotations.Returns;
import com.github.kklisura.cdt.protocol.types.webauthn.Credential;
import com.github.kklisura.cdt.protocol.types.webauthn.VirtualAuthenticatorOptions;
import java.util.List;

/** This domain allows configuring virtual authenticators to test the WebAuthn API. */
@Experimental
public interface WebAuthn {

  /**
   * Enable the WebAuthn domain and start intercepting credential storage and retrieval with a
   * virtual authenticator.
   */
  void enable();

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
}
