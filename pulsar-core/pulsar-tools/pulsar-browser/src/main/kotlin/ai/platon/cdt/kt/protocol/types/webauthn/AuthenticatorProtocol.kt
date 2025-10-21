package ai.platon.cdt.kt.protocol.types.webauthn

import com.fasterxml.jackson.`annotation`.JsonProperty

public enum class AuthenticatorProtocol {
  @JsonProperty("u2f")
  U_2F,
  @JsonProperty("ctap2")
  CTAP_2,
}
