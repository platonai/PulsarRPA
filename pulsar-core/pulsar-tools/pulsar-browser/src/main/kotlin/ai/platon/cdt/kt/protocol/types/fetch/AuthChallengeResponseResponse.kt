@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.fetch

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * The decision on what to do in response to the authorization challenge.  Default means
 * deferring to the default behavior of the net stack, which will likely either the Cancel
 * authentication or display a popup dialog box.
 */
public enum class AuthChallengeResponseResponse {
  @JsonProperty("Default")
  DEFAULT,
  @JsonProperty("CancelAuth")
  CANCEL_AUTH,
  @JsonProperty("ProvideCredentials")
  PROVIDE_CREDENTIALS,
}
