package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.annotations.ReturnTypeParameter
import ai.platon.cdt.kt.protocol.support.annotations.Returns
import ai.platon.cdt.kt.protocol.types.webauthn.Credential
import ai.platon.cdt.kt.protocol.types.webauthn.VirtualAuthenticatorOptions
import kotlin.Boolean
import kotlin.String
import kotlin.collections.List

/**
 * This domain allows configuring virtual authenticators to test the WebAuthn
 * API.
 */
@Experimental
public interface WebAuthn {
  /**
   * Enable the WebAuthn domain and start intercepting credential storage and
   * retrieval with a virtual authenticator.
   */
  public suspend fun enable()

  /**
   * Disable the WebAuthn domain.
   */
  public suspend fun disable()

  /**
   * Creates and adds a virtual authenticator.
   * @param options
   */
  @Returns("authenticatorId")
  public suspend fun addVirtualAuthenticator(@ParamName("options")
      options: VirtualAuthenticatorOptions): String

  /**
   * Removes the given authenticator.
   * @param authenticatorId
   */
  public suspend fun removeVirtualAuthenticator(@ParamName("authenticatorId")
      authenticatorId: String)

  /**
   * Adds the credential to the specified authenticator.
   * @param authenticatorId
   * @param credential
   */
  public suspend fun addCredential(@ParamName("authenticatorId") authenticatorId: String,
      @ParamName("credential") credential: Credential)

  /**
   * Returns a single credential stored in the given virtual authenticator that
   * matches the credential ID.
   * @param authenticatorId
   * @param credentialId
   */
  @Returns("credential")
  public suspend fun getCredential(@ParamName("authenticatorId") authenticatorId: String,
      @ParamName("credentialId") credentialId: String): Credential

  /**
   * Returns all the credentials stored in the given virtual authenticator.
   * @param authenticatorId
   */
  @Returns("credentials")
  @ReturnTypeParameter(Credential::class)
  public suspend fun getCredentials(@ParamName("authenticatorId") authenticatorId: String):
      List<Credential>

  /**
   * Removes a credential from the authenticator.
   * @param authenticatorId
   * @param credentialId
   */
  public suspend fun removeCredential(@ParamName("authenticatorId") authenticatorId: String,
      @ParamName("credentialId") credentialId: String)

  /**
   * Clears all the credentials from the specified device.
   * @param authenticatorId
   */
  public suspend fun clearCredentials(@ParamName("authenticatorId") authenticatorId: String)

  /**
   * Sets whether User Verification succeeds or fails for an authenticator.
   * The default is true.
   * @param authenticatorId
   * @param isUserVerified
   */
  public suspend fun setUserVerified(@ParamName("authenticatorId") authenticatorId: String,
      @ParamName("isUserVerified") isUserVerified: Boolean)

  /**
   * Sets whether tests of user presence will succeed immediately (if true) or fail to resolve (if
   * false) for an authenticator.
   * The default is true.
   * @param authenticatorId
   * @param enabled
   */
  public suspend fun setAutomaticPresenceSimulation(@ParamName("authenticatorId")
      authenticatorId: String, @ParamName("enabled") enabled: Boolean)
}
